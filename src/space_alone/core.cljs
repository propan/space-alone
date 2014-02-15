(ns space-alone.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<! put! chan filter< map<]]
            [clojure.browser.dom :as dom]
            [goog.events :as events]
            [space-alone.control :as ctrl]
            [space-alone.constants :as C]
            [space-alone.draw :as d]
            [space-alone.models :as m]
            [space-alone.tick :as t]))

(enable-console-print!)

(def controls-start
  {C/N_KEY       :start-game
   C/ARROW_LEFT  :start-rotate-left
   C/ARROW_RIGHT :start-rotate-right
   C/ARROW_UP    :start-accelerate
   C/SPACE       :start-shooting})

(def controls-stop
  {C/ARROW_LEFT  :stop-rotate-left
   C/ARROW_RIGHT :stop-rotate-right
   C/ARROW_UP    :stop-accelerate
   C/SPACE       :stop-shooting})

(def state (atom (m/welcome-screen)))

(def canvas (dom/get-element "open-space"))
(def ctx (.getContext canvas "2d"))

(defn listen
  [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn raf-fn
  [last-time callback element]
  (let [current-time (.getTime (js/Date.))
        timeout      (Math/max 0 (- 16 (- current-time last-time)))
        next-call    (+ current-time timeout)]
    (.setTimeout js/window #(callback next-call) timeout)
    (aset js/window "requestAnimationFrame" (partial raf-fn next-call))))

(defn init-raf
  "Based on http://paulirish.com/2011/requestanimationframe-for-smart-animating"
  []
  (let [last-time 0]
    ;; resolve vendors rAF 
    (loop [afn     (.-requestAnimationFrame js/window)
           vendors ["ms" "webkit" "moz" "o"]]
      (when-not (or (not (nil? afn))
                    (empty? vendors))
        (let [vendor (first vendors)
              afn    (aget js/window (str vendor "RequestAnimationFrame"))]
          (println vendor)
          (aset js/window "requestAnimationFrame" afn)
          (aset js/window "cancelAnimationFrame" (or (aget js/window (str vendor "CancelAnimationFrame"))
                                                     (aget js/window (str vendor "CancelRequestAnimationFrame"))))
          (recur afn (rest vendors)))))
    ;; improvise, if it's not resolved
    (when-not (.-requestAnimationFrame js/window)
      (aset js/window "requestAnimationFrame" (partial raf-fn 0)))
    (when-not (.-cancelAnimationFrame js/window)
      (aset js/window "cancelAnimationFrame" js/clearTimeout))))

(defn draw-loop
  [then time]
  (swap! state t/tick)
  (d/draw @state ctx)
  (.requestAnimationFrame js/window #(draw-loop time %)))

(defn- to-event
  [mapping]
  (fn [e]
    (get mapping (.-keyCode e) :unknown)))

(defn event-loop
  []
  (let [events (filter< #(not (= :unknown %))
                           (a/merge [(map< (to-event controls-start) (listen js/document events/EventType.KEYDOWN))
                                     (map< (to-event controls-stop) (listen js/document events/EventType.KEYUP))]))]
    (go (while true
          (swap! state ctrl/handle (<! events))))))

(init-raf)
(event-loop)
(draw-loop (.getTime (js/Date.)) (+ 10 (.getTime (js/Date.))) )
