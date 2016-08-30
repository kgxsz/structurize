(ns structurize.system.state
  (:require [structurize.system.utils :as u]
            [com.stuartsierra.component :as component]
            [cljs-time.core :as t]
            [reagent.core :as r]
            [traversy.lens :as l]
            [taoensso.timbre :as log]))


;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn track
  "Derefs a reagent track into the app at current track index.
   If in a tooling context, the track will act from the root of state.

   Params:
   v - traversy view or view-single
   +lens - The lens into the app at current track-index (or state root if in a tooling context)
   f - the function whose output change will determine whether the track is triggered"

  ([Φ v +lens] (track Φ v +lens identity))
  ([{:keys [!state context] :as Φ} v +lens f]
   (if (:tooling? context)
     @(r/track #(f (v @!state +lens)))
     @(r/track #(let [index (get-in @!state [:tooling :track-index])]
                 (f (v @!state (l/*> (l/in [:app-history index]) +lens))))))))


(defn read
  "Reads into the app at current read-write index.
   If in a tooling context, the read will act from the root of state.

   Params:
   v - traversy view or view-single
   +lens - The lens into the app at current track-index (or state root if in a tooling context)"
  [{:keys [!state context] :as Φ} v +lens]

  (if (:tooling? context)
    (v @!state +lens)
    (let [index (get-in @!state [:tooling :read-write-index])]
      (v @!state (l/*> (l/in [:app-history index]) +lens)))))


(defn write!
  "Writes into the app at current read-write index.
   If in a tooling context, the write will act from the root of state.

   Params:
   id - the write id, used in tooling
   f - the mutating function"
  [{:keys [config-opts !state context] :as Φ} id f]
  (if (:tooling? context)
    (let [log? (get-in config-opts [:tooling :log?])]
      (when log? (log/debug "write:" id))
      (swap! !state f))
    (let [state @!state
          index (get-in state [:tooling :read-write-index])
          time-travelling? (get-in state [:tooling :time-travelling?])
          pre-app (get-in state [:app-history index])
          post-app (f pre-app)
          paths (u/make-paths post-app pre-app)
          upstream-paths (u/make-upstream-paths paths)]

      (log/debug "write:" id)

      (swap! !state #(cond-> %
                       true (update-in [:tooling :read-write-index] inc)
                       (not time-travelling?) (update-in [:tooling :track-index] inc)
                       true (assoc-in [:tooling :writes (inc index)] {:id id
                                                                      :n (inc index)
                                                                      :paths paths
                                                                      :upstream-paths upstream-paths
                                                                      :t (t/now)})
                       (not time-travelling?) (assoc-in [:tooling :app-browser-props :written] {:paths paths
                                                                                                :upstream-paths upstream-paths})
                       true (assoc-in [:app-history (inc index)] post-app))))))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-!state [config-opts]
  (r/atom {:app-history {0 {:playground {:heart 0
                                         :star 3
                                         :ping 0
                                         :pong 0}
                            :location {:path nil
                                       :handler :unknown
                                       :query nil}
                            :app-status :uninitialised
                            :comms {:chsk-status :initialising
                                    :message {}
                                    :post {}}
                            :auth {}}}

           :tooling {:track-index 0
                     :read-write-index 0
                     :time-travelling? false
                     :tooling-slide-over {:open? true}
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
    (assoc component :!state (make-!state config-opts)))
  (stop [component] component))
