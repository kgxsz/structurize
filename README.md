#Structurize

#####A repository to mess around with Component, Sente, and other goods.
[![Stories in Ready](https://badge.waffle.io/kgxsz/structurize.svg?label=ready&title=Ready)](http://waffle.io/kgxsz/structurize)
[![Build Status](https://travis-ci.org/kgxsz/structurize.svg?branch=master)](https://travis-ci.org/kgxsz/structurize)


## Local development setup
- Ensure that you have setup the private configuration file: `~/.lein/structurize/config.edn`.
- To start the back end: `lein repl :headless`.
- To start the front end: `lein figwheel`.
- To compile the css on file changes: `lein garden auto`.
- To see the front end, hit `localhost:3000`.
- To connect to the back end nREPL, hit port `4000`.
- To get a cljs repl up and running:
  - connect to port `5000`.
  - then: `(use 'figwheel-sidecar.repl-api)`.
  - finally: `(cljs-repl)`.

## In the cloud
- We compile and test on Travis with: `lein with-profile travis test`.
- We run on Heroku with: `lein with-profile heroku run -m structurize.main`.
