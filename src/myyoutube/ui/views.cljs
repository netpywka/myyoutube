(ns myyoutube.ui.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [myyoutube.youtube-api :as api]
            [re-frame.core :as re-frame]
            [clojure.string :as string]))

(defn list-of-videos [items mctrclr block?]
  [c/view {:overflow-y :scroll :padding-right 15}
   (for [{:keys [snippet id]} items]
     ^{:key snippet}
     [:div {:style {:margin 0 :margin-top 10}}
      [c/touchable {:on-press #(.open js/window (str "https://www.youtube.com/watch?v=" id) "_blank")}
       [:img {:src (get-in snippet [:thumbnails :medium :url]) :width 320 :height 180}]]
      [c/view {:flex-direction :row :max-width 320}
       [:div {:style {:font-weight :bold :font-size 13 :color mctrclr}}
        (:channelTitle snippet)
        [:div {:style {:font-weight :normal :font-size 13 :color mctrclr}} (:title snippet)]]
       [c/view {:flex 1}]
       (when block?
         [c/touchable {:on-press #(re-frame/dispatch [:block-channel (:channelId snippet)])} "🚫"])]])])

(defview popular [code mctrclr]
  (letsubs [items [:popular-filtered code]]
    [c/view {:margin-right 10 :overflow :hidden :padding-top 10}
     [:div {:style {:color mctrclr :font-size 15}} "Popular "code" videos: " (count items)]
     [list-of-videos items mctrclr true]]))

(defview subscr [mctrclr]
 (letsubs [items [:sorted-subscriptions]]
   [c/view {:margin-right 10 :overflow :hidden :padding-top 10}
    [:div {:style {:color mctrclr :font-size 15}} "Subscriptions: " (count items)]
    [list-of-videos items mctrclr false]]))

(defview main-view []
  (letsubs [signed-in? [:get :signed-in?]
            form [:get :show-form]
            initialized? [:get :initialized]
            filter [:filter]
            bg [:get :bg]]
    (let [mclr  (if bg :black :white)
          mctrclr (if bg :white :black)]
      [c/view {:flex 1 :padding 10 :background-color mclr}
       (if signed-in?
         [c/view {:flex 1}
          [c/view {:flex-direction :row :align-items :center}
           [:img {:src "parsley.png" :height 50}]
           [c/view {:margin-top 10 :margin-bottom 10 :margin-left 10 :color mctrclr} "Blocked channels: "]
           [c/button {:on-press #(re-frame/dispatch [:set :show-form (when (not= form :filters) :filters)])}  (count filter)]
           [c/view {:flex 1}]
           [c/touchable {:on-press #(re-frame/dispatch [:set :bg (not bg)])}
            [c/view {:flex 1 :background-color mctrclr :border-radius "4px" :padding 5 :color mctrclr
                     :margin-right 10}
             "C"]]
           [c/button {:on-press #(api/sing-out)} "Sign out"]]
          (cond (= form :filters)
                [c/view {:margin-top 5 :margin-bottom 5}
                 [:textarea {:readonly false :default-value (string/join " " filter)
                             :on-change #(re-frame/dispatch [:set :filters-edit (.-value (.-target %))])}]
                 [c/button {:on-press #(re-frame/dispatch [:save-filters]) :style {:width 150}} "Save"]])
          [c/view {:flex-direction :row :flex 1}
           [popular "RU" mctrclr]
           [popular "US" mctrclr]
           [subscr mctrclr]]]
         [c/view {:align-items :center :justify-content :center :flex 1}
          (if initialized?
            [c/button {:on-press #(api/sing-in)} "Sign in!"]
            [c/view {:flex-direction :row}
             [:input {:type :text :placeholder "Goodle API CLIENT ID"
                      :on-change #(re-frame/dispatch [:set :client-id (.-value (.-target %))])}]
             [c/button {:on-press #(re-frame/dispatch [:init-client])} "Init client"]])])])))