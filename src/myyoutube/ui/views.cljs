(ns myyoutube.ui.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [myyoutube.youtube-api :as api]
            [re-frame.core :as re-frame]
            [myyoutube.ui.popup.views :as popup]
            [myyoutube.ui.items.views :as items]))

(defview filter-view []
  (letsubs [filter [:storage/filter]]
    [c/button {:on-press #(re-frame/dispatch [:set :settings-form {:type :filters :title "Blocked channels"}])}
     (count filter)]))

(defview quota-view [oppo-color]
  (letsubs [{:keys [number]} [:storage/quota]]
    [c/view {:margin-top 10 :margin-bottom 10 :margin-left 20 :color oppo-color}
     "Used quota: " (or number "0") " of 10000 / day"]))

(defview main-view []
  (letsubs [client-id             [:storage/client-id]
            bg                    [:storage/bg]
            signed-in?            [:get :signed-in?]
            initialized?          [:get :initialized?]
            initialization-failed [:get :initialization-failed]
            color                 [:color]
            oppo-color            [:oppo-color]]
    [c/view {:flex 1 :padding 10 :background-color color}
     (if signed-in?
       [c/view {:flex 1}
        [c/view {:flex-direction :row :align-items :center}
         [:img {:src "parsley.png" :height 50}]
         [c/view {:margin-top 10 :margin-bottom 10 :margin-left 10 :color oppo-color} "Blocked channels: "]
         [filter-view]
         [quota-view oppo-color]
         [c/view {:flex 1}]
         [c/button {:on-press #(re-frame/dispatch [:set :settings-form {:type  :add-new
                                                                        :title "Add new"
                                                                        :data  {:type :popular :country "AF"}}])
                    :style    {:margin-right 20}} "+"]
         [c/touchable {:on-press #(re-frame/dispatch [:set-in [:storage :bg] (not bg)])}
          [c/view {:flex         1 :background-color oppo-color :border-radius "4px" :padding 5 :color oppo-color
                   :margin-right 10}
           "C"]]
         [c/button {:on-press #(api/sing-out)} "Sign out"]]
        [items/items-view]]
       [c/view {:align-items :center :justify-content :center :flex 1}
        (if client-id
          (if initialized?
            [c/view {:flex-direction :row :align-items :center}
             [c/button {:on-press #(re-frame/dispatch [:refresh-client])} "Use another CLIENT ID"]
             [c/view {:padding-right 5 :padding-left 5} " or "]
             [c/button {:on-press #(api/sing-in)} "Sign in!"]]
            (if initialization-failed
              [c/view {:flex-direction :row :align-items :center}
               ;;js/gapi.client may be called only once, so we need to refresh a page
               [c/button {:on-press #(re-frame/dispatch [:refresh-client])} "Use another CLIENT ID"]
               [c/view {:padding-right 5 :padding-left 5} " or "]
               [c/button {:on-press #(re-frame/dispatch [:init-client])} "Init client"]]
              [:img {:src "parsley.png" :height 150}]))
          [:input {:type      :text :placeholder "Paste Goodle API CLIENT ID here"
                   :on-change #(do (re-frame/dispatch [:ls/store :client-id (.-value (.-target %))])
                                   (re-frame/dispatch [:set :initialization-failed true]))}])])
     [popup/popup]]))