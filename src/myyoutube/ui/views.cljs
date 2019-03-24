(ns myyoutube.ui.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [myyoutube.youtube-api :as api]
            [re-frame.core :as re-frame]
            [clojure.string :as string]))

(defview popular [code]
  (letsubs [items [:popular-filtered code]]
    [c/view {:margin-right 10 :overflow :hidden}
     [:div "Popular ("code") videos: " (count items)]
     [c/view {:overflow-y :scroll :padding-right 15}
      (for [{:keys [snippet id]} items]
        ^{:key snippet}
        [:div {:style {:margin 0 :margin-top 10}}
         [c/touchable {:on-press #(.open js/window (str "https://www.youtube.com/watch?v=" id) "_blank")}
          [:img {:src (get-in snippet [:thumbnails :medium :url]) :width 320 :height 180}]]
         [c/view {:flex-direction :row :max-width 320}
          [:div {:style {:font-weight :bold :font-size 13}}
           (:channelTitle snippet)
           [:div {:style {:font-weight :normal :font-size 13}} (:title snippet)]]
          [c/view {:flex 1}]
          [c/touchable {:on-press #(re-frame/dispatch [:block-channel (:channelId snippet)])} "🚫"]]])]]))

(defview main-view []
  (letsubs [signed-in? [:get :signed-in?]
            form [:get :show-form]
            initialized? [:get :initialized]
            filter [:filter]]
    [c/view {:flex 1 :margin 10}
     (if signed-in?
       [c/view {:flex 1}
        [c/view {:flex-direction :row}
         [c/touchable {:on-press #(re-frame/dispatch [:set :show-form (when (not= form :filters) :filters)])}
          [c/view {:margin-top 10 :margin-bottom 10} "Blocked channels: " (count filter)]]
         [c/view {:flex 1}]
         [c/button {:on-press #(api/sing-out)} "Sign out"]]
        (cond (= form :filters)
              [c/view {:margin-top 5 :margin-bottom 5}
               [:textarea {:readonly false :default-value (string/join " " filter)
                           :on-change #(re-frame/dispatch [:set :filters-edit (.-value (.-target %))])}]
               [c/button {:on-press #(re-frame/dispatch [:save-filters]) :style {:width 150}} "Save"]])
        [c/view {:flex-direction :row :flex 1}
         [popular "RU"]
         [popular "US"]]]
       [c/view {:align-items :center :justify-content :center :flex 1}
        (if initialized?
          [c/button {:on-press #(api/sing-in)} "Sign in!"]
          [c/view {:flex-direction :row}
           [:input {:type :text :placeholder "Goodle API CLIENT ID"
                    :on-change #(re-frame/dispatch [:set :client-id (.-value (.-target %))])}]
           [c/button {:on-press #(re-frame/dispatch [:init-client])} "Init client"]])])]))