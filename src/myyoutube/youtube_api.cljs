(ns myyoutube.youtube-api
  (:require [re-frame.core :as re-frame]))

(defn auth-instance []
  (.getAuthInstance js/gapi.auth2))

(defn init [client-id update-sign-status]
  (-> (.init js/gapi.client
             (clj->js {:discoveryDocs ["https://www.googleapis.com/discovery/v1/apis/youtube/v3/rest"]
                       :clientId      client-id
                       :scope         "https://www.googleapis.com/auth/youtube.readonly"}))
      (.then (fn []
               (let [signed-in? (.-isSignedIn (auth-instance))]
                 (.listen signed-in? update-sign-status)
                 (update-sign-status (.get signed-in?)))))))

(defn sing-in []
  (.signIn (auth-instance)))

(defn sing-out []
  (.signOut (auth-instance)))

(defn all-list [api params key items]
  (-> (.list api (clj->js params))
      (.then #(let [{:keys [result]} (js->clj % :keywordize-keys true)
                    {:keys [nextPageToken]} result
                    items (into [] (concat items (:items result)))]
                (if nextPageToken
                  (all-list api (assoc params "pageToken" nextPageToken) key items)
                  (re-frame/dispatch [:set-in (concat [:api] key) items]))))))

(defn subscriptions []
  (all-list gapi.client.youtube.subscriptions
            {"mine"       "true"
             "part"       "snippet"
             "maxResults" "50"}
            [:subscriptions]
            []))

(defn popular [code]
  (all-list gapi.client.youtube.videos
            {"chart"      "mostPopular"
             "regionCode" code
             "part"       "snippet,statistics"
             "maxResults" "50"}
            [:popular code]
            []))