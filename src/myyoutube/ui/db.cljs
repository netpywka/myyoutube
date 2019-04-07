(ns myyoutube.ui.db)

(def storage-key :storage)
(def store-keys [:filter :bg :client-id :items :quota :seen])
(def default-values [nil nil nil nil nil #{}])

(def app-db {:initialized?  false
             :signed-in?    false
             :settings-form nil})