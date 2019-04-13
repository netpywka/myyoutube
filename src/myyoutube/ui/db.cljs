(ns myyoutube.ui.db)

(def storage-key :storage)
(def store-keys [:filter :bg :client-id :items :quota :seen :api])
(def default-values [#{} nil nil {} nil #{} {}])

(def app-db {:initialized?          false
             :initialization-failed false
             :signed-in?            false
             :settings-form         nil})