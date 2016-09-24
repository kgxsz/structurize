(ns structurize.components.app-browser
  (:require [structurize.system.utils :as su :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

(declare node-group)


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn node [φ path _]
  (let [toggle-collapsed #(side-effect! φ :app-browser/toggle-node-collapsed
                                        {:path path})
        toggle-focused #(side-effect! φ :app-browser/toggle-node-focused
                                      {:path path})]

    (log-debug φ "mount node:" path)

    (fn [_ _ opts]
      (let [{:keys [tail-braces first? last? downstream-focused?]} opts
            track-index (track φ l/view-single
                               (in [:tooling :track-index]))
            node (track φ l/view-single
                        (l/*> (in [:app-history track-index]) (l/in path)))
            collapsed? (track φ l/view-single
                              (in [:tooling :app-browser-props :collapsed])
                              #(contains? % path))
            written? (track φ l/view-single
                            (in [:tooling :app-browser-props :written :paths])
                            #(contains? % path))
            upstream-written? (track φ l/view-single
                                     (in [:tooling :app-browser-props :written :upstream-paths])
                                     #(contains? % path))
            focused? (track φ l/view-single
                            (in [:tooling :app-browser-props :focused :paths])
                            #(contains? % path))
            upstream-focused? (track φ l/view-single
                                     (in [:tooling :app-browser-props :focused :upstream-paths])
                                     #(contains? % path))
            downstream-focused? (or downstream-focused? focused?)

            k (last path)
            v node
            collapsed-group-node? (and collapsed?
                                       (map? v)
                                       (not (empty? node)))
            empty-group-node? (and (map? v) (empty? node))
            node-value? (not (map? v))
            show-tail-braces? (and last? (or collapsed? empty-group-node? node-value?))
            node-key-class (u/->class {:c-app-browser__node__key--downstream-focused downstream-focused?
                                       :c-app-browser__node__key--first first?
                                       :c-app-browser__node__key--focused (or focused? upstream-focused?)
                                       :c-app-browser__node__key--written (or written? upstream-written?)})
            node-value-class (u/->class {:c-app-browser__node__value--focused (or focused? downstream-focused?)
                                         :c-app-browser__node__value--clickable (or collapsed-group-node? collapsed?)
                                         :c-app-browser__node__value--written (or written? upstream-written?)})]

        (log-debug φ "render node:" path)

        [:div.c-app-browser__node
         [:div.c-app-browser__brace {:class (when-not first? :is-hidden)}
          "{"]


         [:div.c-app-browser__node__key {:class node-key-class
                                         :on-mouse-over (u/without-propagation toggle-focused)
                                         :on-mouse-out (u/without-propagation toggle-focused)
                                         :on-click (u/without-propagation toggle-collapsed)}

          (pr-str k)]

         (cond
           collapsed-group-node? (list [:div.c-app-browser__brace {:key :opening} "{"]
                                       [:div.c-app-browser__node__value {:key k
                                                                         :class node-value-class
                                                                         :on-mouse-over (u/without-propagation toggle-focused)
                                                                         :on-mouse-out (u/without-propagation toggle-focused)
                                                                         :on-click (u/without-propagation toggle-focused toggle-collapsed)}
                                        "~"]
                                       [:div.c-app-browser__brace {:key :closing} "}"])

           empty-group-node? (list [:div.c-app-browser__brace {:key :opening} "{"]
                                   [:div.c-app-browser__brace {:key :closing} "}"])

           collapsed? [:div.c-app-browser__node__value {:class node-value-class
                                                        :on-mouse-over (u/without-propagation toggle-focused)
                                                        :on-mouse-out (u/without-propagation toggle-focused)
                                                        :on-click (u/without-propagation toggle-collapsed)}
                       "~"]

           node-value? [:div.c-app-browser__node__value {:class node-value-class
                                                         :on-mouse-over (u/without-propagation toggle-focused)
                                                         :on-mouse-out (u/without-propagation toggle-focused)}
                        (pr-str v)]

           last? [node-group φ path {:tail-braces (str tail-braces "}") :downstream-focused? downstream-focused?}]

           :else [node-group φ path {:tail-braces "}" :downstream-focused? downstream-focused?}])

         [:div.c-app-browser__brace {:class (when-not show-tail-braces? :is-hidden)}
          tail-braces]]))))


(defn node-group [φ path _]
  (log-debug φ "mount node-group:" path)
  (fn [_ _ opts]
    (let [{:keys [tail-braces downstream-focused?]} opts
          track-index (track φ l/view-single
                             (in [:tooling :track-index]))
          nodes (track φ l/view-single
                       (l/*> (in [:app-history track-index]) (l/in path)))
          num-nodes (count nodes)]

      (log-debug φ "render node-group:" path)

      [:div
       (doall
        (for [[i [k _]] (map-indexed vector nodes)
              :let [first? (zero? i)
                    last? (= num-nodes (inc i))
                    path (conj path k)]]
          [:div {:key (pr-str k)}
           [node φ path {:tail-braces tail-braces :first? first? :last? last? :downstream-focused? downstream-focused?}]]))])))


(defn app-browser [φ]
  (log-debug φ "mount app-browser")
  (fn []
    (log-debug φ "render app-browser")
    [:div.c-app-browser
     [node-group φ [] {:tail-braces "}"}]]))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :app-browser/toggle-node-collapsed
  [Φ id {:keys [path] :as props}]
  (write! Φ :app-browser/toggle-node-collapsed
          (fn [x]
            (update-in x [:tooling :app-browser-props :collapsed]
                       #(if (contains? % path)
                          (disj % path)
                          (conj % path))))))


(defmethod process-side-effect :app-browser/toggle-node-focused
  [Φ id {:keys [path] :as props}]
  (write! Φ :app-browser/toggle-node-focused
          (fn [x]
            (-> x
                (update-in [:tooling :app-browser-props :focused :paths]
                           #(if (empty? %) #{path} #{}))
                (update-in [:tooling :app-browser-props :focused :upstream-paths]
                           #(if (empty? %) (su/make-upstream-paths #{path}) #{}))))))
