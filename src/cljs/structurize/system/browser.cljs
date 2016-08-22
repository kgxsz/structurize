(ns structurize.system.browser
  (:require [bidi.bidi :as b]
            [cemerick.url :refer [map->query query->map]]
            [structurize.system.utils :refer [side-effect!]]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [goog.events :as events]
            [medley.core :as m]
            [taoensso.timbre :as log])
  (:import [goog.history Html5History EventType]))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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


(defn listen-for-navigation [{:keys [config-opts history] :as Φ}]
  (let [handler (fn [g-event]
                  (let [routes (:routes config-opts)
                        token (.getToken history)
                        [path query] (str/split token "?")
                        location (merge {:path path
                                         :query (->> query query->map (m/map-keys keyword))}
                                        (b/match-route routes path))]
                    (log/debug "receiving navigation from browser:" token)
                    (when-not (.-isNavigation g-event)
                      (js/window.scrollTo 0 0))
                    (side-effect! Φ :browser/change-location
                                  {:location location})))]
    (doto history
      (events/listen EventType.NAVIGATE #(handler %))
      (.setEnabled true))))


;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Browser [config-opts state side-effector]
  component/Lifecycle

  (start [component]
    (log/info "initialising browser")
    (let [history (make-history)
          φ {:context {:browser? true}
             :config-opts config-opts
             :!state (:!state state)
             :<side-effects (:<side-effects side-effector)
             :history history}]
      (log/info "begin listening for navigation from the browser")
      (listen-for-navigation φ)
      (assoc component :history history)))

  (stop [component] component))

