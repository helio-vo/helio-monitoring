package eu.heliovo.monitoring.action;

/**
 * This is a common interface for all actions. It is a design pattern also called "Command" from the book
 * "Design Patterns" of Gamma et. al. It can be used for logging, so every action executed results in one or more log
 * entries (maybe with the help of pointscuts/AOP). But as well it structured the code.
 * 
 * @author Kevin Seidler
 * 
 */
public interface Action {
}