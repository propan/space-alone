(ns space-alone.tick
  (:require [space-alone.constants :as C]
            [space-alone.models :refer [Asteroid Bullet Ship]]))

(defprotocol Tickable
  (tick [_]))

(defn- correct-position
  [pos max-pos]
  (cond
   (>= pos max-pos) 0
   (< pos 0)        (- max-pos 1)
   :default         pos))

(extend-type Asteroid
  Tickable
  (tick [{:keys [x y vX vY] :as asteroid}]
    (merge asteroid {:x (correct-position (+ x vX) C/SCREEN_WIDTH) 
                     :y (correct-position (+ y vY) C/SCREEN_HEIGHT)})))

;; TODO: replace bullet rotation with vX and vY
(extend-type Bullet
  Tickable
  (tick [{:keys [x y energy rotation] :as bullet}]
    (let [next-x (- x (* C/BULLET_SPEED (Math/sin (* rotation (- C/RAD_FACTOR)))))
          next-y (- y (* C/BULLET_SPEED (Math/cos (* rotation (- C/RAD_FACTOR)))))]
      (merge bullet {:x      (correct-position next-x C/SCREEN_WIDTH)
                     :y      (correct-position next-y C/SCREEN_HEIGHT)       
                     :energy (dec energy)}))))

(defn- next-position
  [position dFn velocity max-position]
  (let [next (dFn position velocity)]
    (correct-position next max-position)))

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
