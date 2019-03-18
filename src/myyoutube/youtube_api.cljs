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

(defn subscriptions []
  (-> (.list gapi.client.youtube.subscriptions
             #js {"mine" "true"
                  "part" "snippet,contentDetails"
                  "maxResults" "50"})
      (.then #(re-frame/dispatch [:set-in [:api :subscriptions] (get-in (js->clj % :keywordize-keys true) [:result :items])]))))

(defn popular [code]
  (-> (.list gapi.client.youtube.videos
             #js {"chart" "mostPopular"
                  "regionCode" code
                  "part" "snippet"
                  "maxResults" "50"})
      (.then #(re-frame/dispatch [:set-in [:api :popular code] (get-in (js->clj % :keywordize-keys true) [:result :items])]))))