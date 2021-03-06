(ns structurize.system.config-opts
  (:require [structurize.public-config :refer [public-config]]
            [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [camel-snake-kebab.core :as csk]))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-config
  "Returns a function that takes a kw and first checks if an env var exists,
   before going to the config map to get a value."
  []

  (let [home (System/getProperty "user.home")
        private-config (try
                         (-> (str home "/.lein/structurize/private_config.edn") slurp edn/read-string)
                         (catch java.io.FileNotFoundException e {}))
        config (merge public-config private-config)]

    (fn [kw {:keys [type]}]
      (if-let [v (-> kw name csk/->SCREAMING_SNAKE_CASE System/getenv)]
        (case type
          :integer (edn/read-string v)
          :string v)
        (get config kw)))))


;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (let [config (make-config)]
      (assoc component
             :github-auth {:client-id (config :github-auth-client-id {:type :string})
                           :client-secret (config :github-auth-client-secret {:type :string})
                           :redirect-prefix (config :github-auth-redirect-prefix {:type :string})
                           :scope "user:email"}

             :comms {:chsk-opts {:packer :edn}}

             :server {:http-kit-opts {:port (config :port {:type :integer})}
                      :middleware-opts {:params {:urlencoded true
                                                 :nested true
                                                 :keywordize true}
                                        :security {:anti-forgery {:read-token (fn [req] (-> req :params :csrf-token))}
                                                   :xss-protection {:enable? true, :mode :block}
                                                   :frame-options :sameorigin
                                                   :content-type-options :nosniff}
                                        :session {:flash true
                                                  :cookie-attrs {:http-only true
                                                                 :max-age 3600}}
                                        :static {:resources "public"}
                                        :responses {:not-modified-responses true
                                                    :absolute-redirects true
                                                    :content-types true
                                                    :default-charset "utf-8"}}})))

  (stop [component] component))
