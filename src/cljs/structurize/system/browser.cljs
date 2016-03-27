(ns structurize.system.browser
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [cemerick.url :refer [map->query query->map]]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [goog.events :as events]
            [medley.core :as m]
            [taoensso.timbre :as log])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; history setup


(defn make-navigation-handler

  "Returns a function that handles browser navigation
   events and emits the location-change event, which will
   cause an update to the location information in the state."

  [history emit-event!]

  (fn [g-event]
    (let [token (.getToken history)
          [path query] (str/split token "?")
          location (merge {:path path
                           :query (->> query query->map (m/map-keys keyword))}
                          (b/match-route routes path))]
      (log/debug "received navigation from browser:" token)
      (when-not (.-isNavigation g-event) (js/window.scrollTo 0 0))
      (emit-event! [:location-change {:Δ (fn [core] (assoc core :location location))}]))))


(defn make-transformer
  "Custom transformer required to manage query parameters."
  []
  (let [transformer (Html5History.TokenTransformer.)]
    (set! transformer.retrieveToken
          (fn [path-prefix location]
            (str (.-pathname location) (.-search location))))
    (set! transformer.createUrl
          (fn [token path-prefix location]
            (str path-prefix token)))
    transformer))


(defn make-history []
  (doto (Html5History. js/window (make-transformer))
    (.setPathPrefix "")
    (.setUseFragment false)))


(defn listen-for-navigation [history handler]
  (doto history
    (goog.events/listen EventType.NAVIGATE #(handler %))
    (.setEnabled true)))


(defn make-change-location!

  "Returns a function that takes a map of options and updates the
   browser's location accordingly. The browser will fire a navigation
   event if the location changes, which will be dealt with by a listener.

   The returned function expects:

   prefix - the part before the path, set it if you want to navigate to a different site
   path - the path you wish to navigate to
   query - map of query params
   replace? - ensures that the browser replaces the current location in history"

  [history]

  (fn [{:keys [prefix path query replace?]}]
    (let [query-string (when-not (str/blank? (map->query query)) (str "?" (map->query query)))
          current-path (-> (.getToken history) (str/split "?") first)
          token (str (or path current-path) query-string)]
      (log/debug "dispatching change of location to browser:" (str prefix token))
      (cond
        prefix (set! js/window.location (str prefix token))
        replace? (.replaceToken history token)
        :else (.setToken history token)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Browser [config-opts machine]
  component/Lifecycle

  (start [component]
    (log/info "initialising browser")
    (let [history (make-history)]

      (log/info "begin listening for navigation from the browser")
      (listen-for-navigation history (make-navigation-handler history (:emit-event! machine)))

      (assoc component
             :change-location! (make-change-location! history))))

  (stop [component] component))

