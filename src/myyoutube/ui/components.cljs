(ns myyoutube.ui.components)

(defn view [& items]
  (let [styles? (map? (first items))]
    (vec (concat [:div {:style (merge {:display :flex :flex-direction :column} (when styles? (first items)))}]
                 (vec (if styles? (rest items) items))))))

(defn button [{:keys [on-press style]} label]
  [:div {:style (merge {:background-color :lightgreen :cursor :pointer :display :flex :flex-direction :column} style)
         :on-click on-press} label])

(defn touchable [{:keys [on-press]} & items]
  (vec (concat [:div {:style {:cursor :pointer}
                      :on-click on-press}]
               (vec items))))