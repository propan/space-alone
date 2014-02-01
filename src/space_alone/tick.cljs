(ns space-alone.tick
  (:require [space-alone.constants :as C]
            [space-alone.models :refer [Asteroid Bullet Ship]]))

(defn- next-position
  [position dFn velocity max-position]
  (let [next (dFn position velocity)]
    (cond
     (>= next max-position) 0
     (< next 0)             (- max-position 1)
     :default               next)))

(defn- next-rotation
  [rotate rotation]
  (case rotate
    :left    (mod (- rotation C/TURN_FACTOR) 360)
    :right   (mod (+ rotation C/TURN_FACTOR) 360)
    rotation))

(defn- next-thrust
  [accelerate thrust]
  (if accelerate
    (min (+ thrust C/ACCELERATION) C/MAX_THRUST)
    (max 0 (- thrust C/THRUST_DECLINE))))

(defn- next-velocity
  [vFn velocity accelerate rotation thrust]
  (if accelerate
    (let [next-velocity (+ velocity (* thrust (vFn (* rotation C/RAD_FACTOR))))]
      (min (max next-velocity (- C/MAX_VELOCITY)) C/MAX_VELOCITY))
    velocity))

(defprotocol Tickable
  (tick [_]))

(extend-type Asteroid
  Tickable
  (tick [{:keys [x y vX vY] :as asteroid}]
    (merge asteroid {:x (next-position x + vX C/SCREEN_WIDTH)
                     :y (next-position y + vY C/SCREEN_HEIGHT)})))

(extend-type Bullet
  Tickable
  (tick [{:keys [x y vX vY energy] :as bullet}]
    (merge bullet {:x      (next-position x - vX C/SCREEN_WIDTH)
                   :y      (next-position y - vY C/SCREEN_HEIGHT)
                   :energy (dec energy)})))

(extend-type Ship
  Tickable
  (tick [{:keys [x y vX vY rotation thrust accelerate rotate shoot next-shoot] :as ship}]
    (let [shoot? (and shoot (zero? next-shoot))]
      (merge ship {:x          (next-position x + vX C/SCREEN_WIDTH)
                   :y          (next-position y - vY C/SCREEN_HEIGHT)
                   :vX         (next-velocity Math/sin vX accelerate rotation thrust)
                   :vY         (next-velocity Math/cos vY accelerate rotation thrust)
                   :rotation   (next-rotation rotate rotation)
                   :thrust     (next-thrust accelerate thrust)
                   :next-shoot (if shoot?
                                 C/TIME_BETWEEN_SHOOTS
                                 (max 0 (dec next-shoot)))}))))
