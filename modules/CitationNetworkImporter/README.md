# Citation Network Importer

Build a citation network in Gephi without an intermediate file. Search OpenAlex or start from one work, follow citations for as many generations as you ask for, and get a directed graph you can lay out and filter straight away.

An edge runs from the CITED work to the CITING work, which is the direction a claim travels through a literature. Note what that means for Gephi's degree statistics: **out-degree is how often a work was cited** and in-degree is how many of its own references are in the graph. That is the opposite of the convention most bibliometric tools use, and it is deliberate.

## What it adds to Gephi

```
File > Import Database > Citation network
```

Gephi calls that menu item Import Database and it opens the Import Wizard, whose first step asks for a Category. This plugin registers under Bibliographic sources.

One wizard screen collects everything:

| Field | What it does |
|:--|:--|
| Search query | Free text. Leave it empty when you give one work below. |
| Or one work | An OpenAlex identifier such as W3198910543, or a digital object identifier (DOI) such as 10.1016/s2352-3026(21)00193-9. |
| Seed works from the search | How many results from the query start the walk. |
| Follow | Works citing these, works cited by these, or both. |
| Generations | How far to walk out from the seeds. |
| Maximum works | A hard cap, so a hub paper cannot fill the workspace. |
| Neighbours per work | How many citations to take from any one work. |
| Your email address | Optional. OpenAlex serves a faster pool to requests that carry one. |
| OpenAlex key | Optional. A free key removes the rate limit that applies to anonymous searches. |

The address and the key are remembered between sessions in your own preferences. Neither is written into the plugin.

## Node attributes

Every node carries what the source holds, and nothing is inferred:

```
label              the title
doi                the digital object identifier, without the resolver prefix
year               publication year
venue              the journal or other primary source
cited_by_count     citations the source counts, at the time of import
open_access        whether the source records an open access copy
retracted          whether the source records a retraction
authors            author names, separated by semicolons
institutions       distinct institution names across the authorships
concepts           the source's own subject concepts
type               the source's own work type, for example article or review
source             which bibliographic source the work came from
generation         0 for a seed, then 1, 2 and so on outwards
```

## A worked example

Start from one paper and collect the literature that cites it. This one is Beutler and Waalen in Blood, asking what the lower limit of normal for blood haemoglobin actually is, a question whose answer every downstream anaemia prevalence estimate inherits:

```
Or one work                 10.1182/blood-2005-07-3046
Follow                      Works citing these
Generations                 1
Maximum works               300
Neighbours per work         50
```

That request builds a graph of the paper and the works citing it, with any citation between two of THOSE works included as well, because the plugin closes citations inside the neighbourhood without asking the source again. In the ten citing works recorded as test fixtures, that step adds seven edges the source was never asked for, so the neighbourhood is not a star.

Raise Generations to 2 and the walk continues outwards from each of those works. A second generation grows quickly, which is what Maximum works is for.

## How it behaves against the source

Three things measured against the live OpenAlex interface on 21 September 2026, rather than taken from documentation:

1. Anonymous free text search can answer status 429 while the search cluster is under load, with a body naming the seconds to wait. Single work lookups and filter queries were unaffected at the same moment.
2. A free key removes that limit, which is why the wizard offers one.
3. Identifiers resolve in batches rather than one request per work, so a walk over fifty references is two requests instead of fifty.

The importer honours both retry signals, the Retry-After header and the retryAfter field in the body.

Two things it does on its own behalf:

```
cache       every response is written under Gephi's own cache directory, in
            citation-network-importer, so a repeated or resumed walk does not
            refetch what it already holds. Deleting that folder costs nothing
throttle    with no key set, requests that actually leave the machine are kept
            about a second apart, because an unauthenticated caller shares a
            pool with everybody else. A cached response is never delayed
```

## The question this was built for

A definition can be a hub. The 1968 World Health Organization scientific group report on nutritional anaemias set the haemoglobin cutoffs that a whole field still classifies patients by. In OpenAlex it is work W4300771061, cited by hundreds of works, carrying no digital object identifier, no authorships and no references of its own.

Import it and the shape is plain: every edge LEAVES it and none arrives, because an edge here runs from the cited work to the citing one. In Gephi's own statistics that reads as out-degree equal to everything you pulled and in-degree zero. That is what a definitional source looks like from inside the literature that inherited it, and it is the reason this plugin exists rather than a general purpose network importer.

The worked example above starts one step along that same chain, at Beutler and Waalen's paper asking what the lower limit of normal actually is. That paper is itself one of the works citing the 1968 report.

```
Or one work                 W4300771061
Follow                      Works citing these
Generations                 1
```

## A graph you can open without building anything

`demo/threshold-neighbourhood.gexf` is the result of the worked example above: the paper, ten works citing it, and the seventeen citations between all eleven. Open it in Gephi, or drag it into [gephi-lite](https://gephi.org/gephi-lite/) in a browser.

It was generated from the two payloads recorded in `src/test/resources/fixtures`, through the same field mapping the plugin uses, and its node and edge counts match what the tests assert for that neighbourhood.

## Building it

```
mvn clean package
mvn -pl modules/CitationNetworkImporter test
mvn org.gephi:gephi-maven-plugin:run
```

The tests run against responses recorded from OpenAlex and trimmed to the fields this plugin reads. No test touches the network.

## Two sources, and what differs between them

The walk talks to a `CitationSource`, and there are two implementations of it. The wizard's first field chooses which.

| | OpenAlex | Semantic Scholar |
|:--|:--|:--|
| Works citing these | yes | yes |
| Works cited by these | yes | yes, unless the publisher withheld the reference list |
| Paging | cursor | offset |
| Key | optional, and removes the search rate limit | optional |
| Extra node columns | institutions, concepts | none of those |
| Citations closed inside the neighbourhood | yes, from the reference lists already returned | no, the payload carries no reference identifiers |

The withheld case is real rather than theoretical. Asking Semantic Scholar for the references of the paper in the worked example returns a null list with a note saying the publisher elided the field. The plugin reports that in the import report, so a missing reference list does not read as a paper that cites nothing.

Crossref or PubMed can be added the same way, which is why the plugin is not named after any one database.

## Licence and attribution

The plugin is MIT. See LICENSE.txt.

The data is not. Each source sets its own terms and they are not restated here, because they change:

- OpenAlex publishes its licence and its citation guidance on its own help site. Cite OpenAlex when you publish a graph built from it.
- The Semantic Scholar application programming interface is governed by a licence agreement from the Allen Institute for Artificial Intelligence, last updated 17 May 2023, at semanticscholar.org/product/api/license. Read it before redistributing anything pulled through it.

## How each source takes a key, measured on 21 September 2026

```
OpenAlex            api_key as a query parameter. A wrong one answers 401
                    with "Invalid or missing API key"
Semantic Scholar    an x-api-key HEADER. As a query parameter it is ignored
                    entirely, so the request silently runs anonymously, and a
                    wrong key in the header answers 403
```

The second one matters: a key passed the wrong way looks like it worked.
