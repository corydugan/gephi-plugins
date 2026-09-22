/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import com.drcorydugan.citationnetwork.graph.CitationGraph;
import com.drcorydugan.citationnetwork.graph.CitationWalker;
import com.drcorydugan.citationnetwork.source.CachingFetcher;
import com.drcorydugan.citationnetwork.source.CitationSource;
import com.drcorydugan.citationnetwork.source.HttpFetcher;
import com.drcorydugan.citationnetwork.source.JdkHttpFetcher;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.ThrottlingFetcher;
import com.drcorydugan.citationnetwork.source.Work;
import com.drcorydugan.citationnetwork.source.openalex.OpenAlexSource;
import com.drcorydugan.citationnetwork.source.semanticscholar.SemanticScholarSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.spi.WizardImporter;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.modules.Places;
import org.openide.util.NbBundle;

/**
 * Builds a citation network in the Gephi workspace from a bibliographic
 * source, with no intermediate file.
 */
public class CitationNetworkImporter implements WizardImporter, LongTask {

    private static final Logger LOGGER =
            Logger.getLogger(CitationNetworkImporter.class.getName());

    /** Node columns written for every work. */
    static final String COLUMN_DOI = "doi";
    static final String COLUMN_YEAR = "year";
    static final String COLUMN_VENUE = "venue";
    static final String COLUMN_CITED_BY = "cited_by_count";
    static final String COLUMN_OPEN_ACCESS = "open_access";
    static final String COLUMN_RETRACTED = "retracted";
    static final String COLUMN_AUTHORS = "authors";
    static final String COLUMN_INSTITUTIONS = "institutions";
    static final String COLUMN_CONCEPTS = "concepts";
    static final String COLUMN_TYPE = "type";
    static final String COLUMN_SOURCE = "source";
    static final String COLUMN_GENERATION = "generation";

    private final ImportSettings settings = new ImportSettings();
    private ContainerLoader container;
    private Report report;
    private ProgressTicket progressTicket;
    private volatile boolean cancelled;

    public ImportSettings getSettings() {
        return settings;
    }

    @Override
    public boolean execute(ContainerLoader loader) {
        this.container = loader;
        this.report = new Report();
        Progress.start(progressTicket);
        Progress.setDisplayName(progressTicket, message("CitationNetworkImporter.progress.name"));

        loader.setEdgeDefault(EdgeDirectionDefault.DIRECTED);
        loader.setAllowSelfLoop(false);
        loader.setAllowParallelEdge(false);
        addColumns(loader);

        try {
            CitationSource source = buildSource();
            CitationWalker walker = new CitationWalker(source, settings.getWalk(), new CitationWalker.Observer() {
                @Override
                public void progress(String step, int nodes, int edges) {
                    Progress.progress(progressTicket,
                            message("CitationNetworkImporter.progress.step",
                                    step, nodes, edges));
                }

                @Override
                public boolean isCancelled() {
                    return cancelled;
                }
            });

            CitationGraph graph = walker.walk();
            write(graph, loader, source.getId());

            for (String problem : walker.getProblems()) {
                report.log(problem);
            }
            if (source instanceof SemanticScholarSource
                    && ((SemanticScholarSource) source).getLastRequestWasElided()) {
                report.log(message("CitationNetworkImporter.report.elided"));
            }
            report.log(message("CitationNetworkImporter.report.imported",
                    graph.nodeCount(), graph.edgeCount(), source.getDisplayName()));
            if (cancelled) {
                report.log(message("CitationNetworkImporter.report.cancelled"));
            }
            return true;
        } catch (SourceException | IOException e) {
            LOGGER.log(Level.WARNING, "the citation network import stopped", e);
            report.log(message("CitationNetworkImporter.report.stopped", e.getMessage()));
            return false;
        } finally {
            Progress.finish(progressTicket);
        }
    }

    private static String message(String key, Object... arguments) {
        return NbBundle.getMessage(CitationNetworkImporter.class, key, arguments);
    }

    /** Spacing between requests when no key is set, so a shared pool is not flooded. */
    static final Duration UNAUTHENTICATED_INTERVAL = Duration.ofMillis(1100);

    private CitationSource buildSource() throws IOException {
        HttpFetcher fetcher = new JdkHttpFetcher(userAgent());
        if (settings.getApiKey().isEmpty()) {
            fetcher = new ThrottlingFetcher(fetcher, UNAUTHENTICATED_INTERVAL);
        }
        Files.createDirectories(cacheDirectory());
        fetcher = new CachingFetcher(fetcher, cacheDirectory());
        if (ImportSettings.SOURCE_SEMANTIC_SCHOLAR.equals(settings.getSourceId())) {
            return new SemanticScholarSource(fetcher, settings.getApiKey());
        }
        return new OpenAlexSource(fetcher, settings.getMailto(), settings.getApiKey());
    }

    /**
     * The platform's own cache directory, so responses land where Gephi keeps
     * everything else it can afford to lose.
     */
    static Path cacheDirectory() {
        return Places.getCacheSubdirectory("citation-network-importer").toPath();
    }

    private String userAgent() {
        String contact = settings.getMailto();
        return "CitationNetworkImporter/1.0 (Gephi plugin"
                + (contact.isEmpty() ? "" : "; mailto:" + contact) + ")";
    }

    private static void addColumns(ContainerLoader loader) {
        loader.addNodeColumn(COLUMN_DOI, String.class);
        loader.addNodeColumn(COLUMN_YEAR, Integer.class);
        loader.addNodeColumn(COLUMN_VENUE, String.class);
        loader.addNodeColumn(COLUMN_CITED_BY, Integer.class);
        loader.addNodeColumn(COLUMN_OPEN_ACCESS, Boolean.class);
        loader.addNodeColumn(COLUMN_RETRACTED, Boolean.class);
        loader.addNodeColumn(COLUMN_AUTHORS, String.class);
        loader.addNodeColumn(COLUMN_INSTITUTIONS, String.class);
        loader.addNodeColumn(COLUMN_CONCEPTS, String.class);
        loader.addNodeColumn(COLUMN_TYPE, String.class);
        loader.addNodeColumn(COLUMN_SOURCE, String.class);
        loader.addNodeColumn(COLUMN_GENERATION, Integer.class);
    }

    private void write(CitationGraph graph, ContainerLoader loader, String sourceId) {
        for (Work work : graph.getWorks()) {
            NodeDraft node = loader.factory().newNodeDraft(work.getId());
            node.setLabel(work.getTitle() == null ? work.getId() : work.getTitle());
            node.setValue(COLUMN_DOI, work.getDoi());
            node.setValue(COLUMN_YEAR, work.getYear());
            node.setValue(COLUMN_VENUE, work.getVenue());
            node.setValue(COLUMN_CITED_BY, work.getCitedByCount());
            node.setValue(COLUMN_OPEN_ACCESS, work.isOpenAccess());
            node.setValue(COLUMN_RETRACTED, work.isRetracted());
            node.setValue(COLUMN_AUTHORS, String.join("; ", work.getAuthors()));
            node.setValue(COLUMN_INSTITUTIONS, String.join("; ", work.getInstitutions()));
            node.setValue(COLUMN_CONCEPTS, String.join("; ", work.getConcepts()));
            node.setValue(COLUMN_TYPE, work.getType());
            node.setValue(COLUMN_SOURCE, sourceId);
            node.setValue(COLUMN_GENERATION, graph.getGeneration(work.getId()));
            loader.addNode(node);
        }
        for (CitationGraph.Citation citation : graph.getCitations()) {
            EdgeDraft edge = loader.factory()
                    .newEdgeDraft(citation.getCitedId() + "->" + citation.getCitingId());
            edge.setSource(loader.getNode(citation.getCitedId()));
            edge.setTarget(loader.getNode(citation.getCitingId()));
            loader.addEdge(edge);
        }
    }

    @Override
    public ContainerLoader getContainer() {
        return container;
    }

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public boolean cancel() {
        cancelled = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }
}
