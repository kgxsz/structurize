(ns structurize.system.state
  (:require [structurize.system.utils :as u]
            [com.stuartsierra.component :as component]
            [cljs-time.core :as t]
            [reagent.core :as r]
            [traversy.lens :as l]
            [taoensso.timbre :as log])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn track
  "Derefs a reagent track into the app at current track index (or tooling if in a tooling context).
   If in a tooling context, the track will act from the root of state.

   Params:
   v - traversy view or view-single
   +lens - The lens into the app at current track-index (or tooling if in a tooling context)
   f - the function whose output change will determine whether the track is triggered"

  ([Φ v +lens] (track Φ v +lens identity))
  ([{:keys [!app-state !tooling-state] :as Φ} v +lens f]
   (if (:tooling? (meta Φ))
     @(r/track #(f (v @!tooling-state +lens)))
     @(r/track #(let [index (get-in @!tooling-state [:tooling :track-index])]
                 (f (v @!app-state (l/*> (l/in [:app-history index]) +lens))))))))


(defn read
  "Reads into the app at current read-write index (or tooling if in a tooling context).
   If in a tooling context, the read will act from the root of state.

   Params:
   v - traversy view or view-single
   +lens - The lens into the app at current track-index (or tooling if in a tooling context)"
  [{:keys [!app-state !tooling-state] :as Φ} v +lens]

  (if (:tooling? (meta Φ))
    (v @!tooling-state +lens)
    (let [index (get-in @!tooling-state [:tooling :read-write-index])]
      (v @!app-state (l/*> (l/in [:app-history index]) +lens)))))


(defn write!
  "Writes into the app at current read-write index (or tooling if in a tooling context).

   Params:
   id - the write id, used in tooling
   f - the mutating function"
  [{:keys [config-opts !app-state !tooling-state] :as Φ} id f]
  (if (:tooling? (meta Φ))
    (do
      (log-debug Φ "write:" id)
      (swap! !tooling-state f))
    (let [app-state @!app-state
          tooling-state @!tooling-state
          index (get-in tooling-state [:tooling :read-write-index])
          time-travelling? (get-in tooling-state [:tooling :time-travelling?])
          pre-app (get-in app-state [:app-history index])
          post-app (f pre-app)
          paths (u/make-paths post-app pre-app)
          upstream-paths (u/make-upstream-paths paths)]

      (log-debug Φ "write:" id)

      (swap! !tooling-state #(cond-> %
                               true (update-in [:tooling :read-write-index] inc)
                               (not time-travelling?) (update-in [:tooling :track-index] inc)
                               true (assoc-in [:tooling :writes (inc index)] {:id id
                                                                              :n (inc index)
                                                                              :paths paths
                                                                              :upstream-paths upstream-paths
                                                                              :t (t/now)})
                               (not time-travelling?) (assoc-in [:tooling :app-browser-props :written] {:paths paths
                                                                                                        :upstream-paths upstream-paths})))

      (swap! !app-state assoc-in [:app-history (inc index)] post-app))))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-!app-state [config-opts]
  (r/atom {:app-history {0 {:location {:path nil
                                       :handler :unknown
                                       :query nil}
                            :count 0
                            :viewport {}
                            :app-status :uninitialised
                            :comms {:chsk-status :initialising
                                    :message {}
                                    :post {}}
                            :auth {}}}}))


(defn make-!tooling-state [config-opts]
  (r/atom {:tooling {:track-index 0
                     :read-write-index 0
                     :time-travelling? false
                     :tooling-slide-over {:open? false}
                     :writes {}
                     :app-browser-props {:written {:paths #{}
                                                   :upstream-paths #{}}
                                         :collapsed #{}
                                         :focused {:paths #{}
                                                   :upstream-paths #{}}}}}))


;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord State [config-opts]
  component/Lifecycle
  (start [component]
    (log/info "initialising state")
    (assoc component
           :!app-state (make-!app-state config-opts)
           :!tooling-state (make-!tooling-state config-opts)))
  (stop [component] component))
