(ns structurize.system.browser
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [write!]]
            [bidi.bidi :as b]
            [cemerick.url :refer [map->query query->map]]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [goog.events :as events]
            [goog.dom :as dom]
            [medley.core :as m]
            [taoensso.timbre :as log])
  (:import goog.history.Html5History
           goog.history.EventType
           goog.dom.ViewportSizeMonitor))

;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn change-location!
  "Updates the browser's location accordingly. The browser will fire a navigation
   event if the location changes, which will be dealt with by a listener.

   Params:
   prefix - the part before the path, set it if you want to navigate to a different site
   path - the path you wish to navigate to
   query - map of query params
   replace? - ensures that the browser replaces the current location in history"
  [{:keys [history] :as Φ} {:keys [prefix path query replace?]}]

  (let [query-string (when-not (str/blank? (map->query query)) (str "?" (map->query query)))
        current-path (-> (.getToken history) (str/split "?") first)
        token (str (or path current-path) query-string)]
    (log/debug "dispatching change of location to browser:" (str prefix token))
    (cond
      prefix (set! js/window.location (str prefix token))
      replace? (.replaceToken history token)
      :else (.setToken history token))))


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


(defn listen-for-resize [{:keys [config-opts] :as Φ}]
  (let [handler (js/window._.debounce (fn []
                                        (side-effect! Φ :browser/resize))
                                      500
                                      #js {:trailing true})]

    ;; trigger an initial resize
    (side-effect! Φ :browser/resize)

    (doto (ViewportSizeMonitor.)
      (events/listen events/EventType.RESIZE handler))))


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
      (log/info "begin listening for resize from the browser")
      (listen-for-resize φ)
      (assoc component :history history)))

  (stop [component] component))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :browser/change-location
  [Φ id {:keys [location] :as props}]
  (write! Φ :browser/change-location
          (fn [x]
            (assoc x :location location))))


(defmethod process-side-effect :browser/resize
  [{:keys [config-opts] :as Φ} id props]
  (let [{:keys [xs sm md lg]} (get-in config-opts [:viewport :breakpoints])
        width (.-width (dom/getViewportSize))
        breakpoint (cond
                     (< width xs) :xs
                     (< width sm) :sm
                     (< width md) :md
                     (< width lg) :lg
                     :else :xl)
        {:keys [max-col-width min-col-width min-col-n]} (get-in config-opts [:viewport :grid])
        gutter (get-in config-opts [:viewport :grid :gutter breakpoint])
        col-n (max min-col-n (quot (- width gutter) (+ min-col-width gutter)))
        col-width (min max-col-width (int (/ (- width (* (inc col-n) gutter)) col-n)))
        margin (- width (* col-n (+ col-width gutter)) gutter)]

    (write! Φ :browser/resize
            (fn [x]
              (assoc x :viewport {:width width
                                  :height (.-height (dom/getViewportSize))
                                  :breakpoint breakpoint
                                  :grid {:col-n col-n
                                         :col-width col-width
                                         :margin margin
                                         :gutter gutter}})))))
