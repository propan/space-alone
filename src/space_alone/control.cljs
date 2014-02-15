(ns space-alone.control
  (:require [space-alone.models :as m :refer [GameScreen GameOverScreen WelcomeScreen]]))

(defn change-ship-state
  [state p from to]
  (update-in state [:ship p] #(if (= % from) to %)))

;;
;; Controller Protocol
;;

(defprotocol Controller
  (handle [_ event]))

(extend-type GameScreen
  Controller
  (handle [state event]
    (case event
      :start-rotate-left  (change-ship-state state :rotate :none :left)
      :stop-rotate-left   (change-ship-state state :rotate :left :none)
      :start-rotate-right (change-ship-state state :rotate :none :right)
      :stop-rotate-right  (change-ship-state state :rotate :right :none)

      :start-accelerate   (change-ship-state state :accelerate false true)
      :stop-accelerate    (change-ship-state state :accelerate true false)
      :start-shooting     (change-ship-state state :shoot false true)
      :stop-shooting      (change-ship-state state :shoot true false)
      state)))

(extend-type GameOverScreen
  Controller
  (handle [state event]
    (case event
      :start-game (m/game-screen)
      state)))

(extend-type WelcomeScreen
  Controller
  (handle [state event]
    (case event
      :start-game (m/game-screen)
      state)))
