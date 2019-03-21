(ns ^:figwheel-hooks myyoutube.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [myyoutube.ui.views :as views]
            myyoutube.ui.events
            myyoutube.ui.subs))

(defn mount-root []
  (reagent/render [views/main-view] (js/document.getElementById "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:initialize])
  (mount-root))

(defn ^:export initClient []
  (re-frame/dispatch [:init-client]))

(defn ^:after-load on-reload []
  (mount-root))