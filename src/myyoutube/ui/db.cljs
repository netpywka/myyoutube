(ns myyoutube.ui.db)

(def storage-key :storage)
(def store-keys [:filter :bg :client-id :items])

(def app-db {:initialized?  false
             :signed-in?    false
             :settings-form nil})