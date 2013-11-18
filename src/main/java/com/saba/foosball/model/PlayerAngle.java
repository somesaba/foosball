package com.saba.foosball.model;
/**
 * 
 * @author saba
 * Player positions have 5 possibilities:
 * 
 * FORWARD_HORINZONTAL means the player is rotated such that their feet points towards the opposing goal, and the ball can pass below
 * FOWARD_ANGLED means the player is rotated such that their feet points towards the opposing goal, and the ball cannot pass below
 * VERTICAL means the player's feet are facing the floor
 * BACKWARDS_ANGLED means the player is rotated such that their feet points towards their own goal, and the ball can pass below
 * BACKWARD_HORIZONTAL means the player is rotated such that their feet points towards their own goal, and the ball cannot pass below
 */
public enum PlayerAngle {
    FORWARD_HORIZONTAL, FORWARD_ANGLED, VERTICAL, BACKWARD_ANGLED, BACKWARD_HORIZONTAL;
}
