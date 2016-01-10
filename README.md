- To kick off the backend: `lein repl :headless`
- To kick off the frontend: `lein figwheel`

- To connect to the backend nREPL, hit port `4000`

- To get a cljs repl up and running:
  - connect to port `5000`
  - then: `(use 'figwheel-sidecar.repl-api)`
  - finally: `(cljs-repl)`
