(ns deckatron.pages.home
  (:require
    [rum.core :as rum]
    [deckatron.util :as u]
    [deckatron.core :as core]))


(enable-console-print!)


(defonce socket nil)


(defonce *decks (atom {})) ;; deck-id => Deck

;; (add-watch *decks ::log (fn [_ _ _ v] (println "Decks:" v)))


(rum/defc deck [deck]
  (let [href (core/->deck-href deck)]
    [:a.slide {:href href
               :on-click (partial core/fake-navigate-url href)}
      [:.slide-inner
        [:.slide-text (:deck/id deck)]]]))


(rum/defc decks-list [decks & [comp]]
  [:.decks-list
    (for [d (sort-by :deck/id decks)]
      (deck d))
    comp])


(rum/defc page < rum/reactive []
  (let [decks (vals (rum/react *decks))]
    [:.page_home
      [:h1 "Intro to Deckatron"]
      (decks-list
        (filter #(= core/user-deckatron (:user/id %)) decks))
     
      [:h1 "Your decks"]
      (decks-list
        (filter #(= core/user-id (:user/id %)) decks)
        [:a.slide {:href "/create-deck"}
          [:.slide-inner
            [:.slide-text "+ Create new deck"]]])
     
      [:h1 "Other people’s decks"]
      (decks-list
        (remove #(#{core/user-id core/user-deckatron} (:user/id %)) decks))]))


(defmethod core/start-page! :home [_ mount-el]
  (println "Loading decks list")
  ;; TODO watch websocket status, reconnect
  (when (nil? socket)
    (set! socket
      (doto (js/WebSocket. (str "ws://" js/location.host "/api/decks"))
        (aset "onmessage"
          (fn [payload]
            (let [data (u/transit->obj (.-data payload))]
              (println "Received:" data)
              (swap! *decks update (:deck/id data) u/patch (:patch data))))))))
  
  (rum/mount (page) mount-el))


(defmethod core/stop-page! :home [_]
  (when socket
    (.close socket)
    (set! socket nil)
    (reset! *decks {})))
