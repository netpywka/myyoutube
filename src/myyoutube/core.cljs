(ns ^:figwheel-hooks myyoutube.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [myyoutube.views :as views]
            myyoutube.events
            myyoutube.subs))

(defn mount-root []
  (reagent/render [views/main-view] (js/document.getElementById "app")))

(defn ^:export updateSigninStatus [signed-in?]
  (re-frame/dispatch [:set :signed-in? signed-in?])
  (when signed-in?
    (re-frame/dispatch [:get-api])))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))

(defn ^:after-load on-reload []
  (mount-root))