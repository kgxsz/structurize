#Structurize

#####A repository to mess around with Component, Sente, and other goods.
[![Stories in Ready](https://badge.waffle.io/kgxsz/structurize.svg?label=ready&title=Ready)](http://waffle.io/kgxsz/structurize)

## Local development setup
- To start the back end: `lein repl :headless`.
- To start the front end: `lein figwheel`.
- To see the front end, hit `localhost:3000`.
- To connect to the back end nREPL, hit port `4000`.
- To get a cljs repl up and running:
  - connect to port `5000`.
  - then: `(use 'figwheel-sidecar.repl-api)`.
  - finally: `(cljs-repl)`.
