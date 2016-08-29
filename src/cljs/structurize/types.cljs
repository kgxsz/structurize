(ns structurize.types
  (:require [cljs.spec :as s]))

(s/def ::url (s/and string? #(re-matches  #"^https?://(www\.)?[a-zA-Z0-9\:]{2,256}\.[a-z]{2,6}/?.*$" %)))

(s/def ::focus fn?)

(s/def ::fmap (s/nilable fn?))

(s/def ::lens (s/keys :req-un [::focus ::fmap]))



