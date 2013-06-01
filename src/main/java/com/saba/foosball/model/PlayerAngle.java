package com.saba.foosball.model;
/**
 * 
 * @author saba
 * Player positions have 5 possibilities:
 * 
 * FORWARD_UP means the player is rotated such that their feet points towards the opposing goal, and the ball can pass below
 * FOWARD_DOWN means the player is rotated such that their feet points towards the opposing goal, and the ball cannot pass below
 * DOWN means the player's feet are facing the floor
 * BACKWARDS_DOWN means the player is rotated such that their feet points towards their own goal, and the ball can pass below
 * FOWARD_UP means the player is rotated such that their feet points towards their own goal, and the ball cannot pass below
 */
public enum PlayerAngle {
    FORWARD_UP, FORWARD_DOWN, DOWN, BACKWARD_DOWN, BACKWARD;
}
