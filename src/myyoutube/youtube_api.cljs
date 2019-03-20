(ns myyoutube.youtube-api
  (:require [re-frame.core :as re-frame]))

(defn sing-in []
  (-> js/gapi.auth2
      .getAuthInstance
      .signIn))

(defn sing-out []
  (-> js/gapi.auth2
      .getAuthInstance
      .signOut))

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
             "part"       "snippet,contentDetails"
             "maxResults" "50"}
            [:subscriptions]
            []))

(defn popular [code]
  (all-list gapi.client.youtube.videos
            {"chart"      "mostPopular"
             "regionCode" code
             "part"       "snippet"
             "maxResults" "50"}
            [:popular code]
            []))