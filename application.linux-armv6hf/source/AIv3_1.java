import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class AIv3_1 extends PApplet {


//-------------------------------------------------------------------------------------------------
//EDITABLE
//-------------------------------------------------------------------------------------------------

//max steps dots are allowed to take
int maxSteps = 1000;

//gens to new goal pos
int genToNew = 5;


//positions the goal can have
int maxGoalSteps = 4;

//positions of the goal
int[][] goals = {{600, 400}, {450, 325}, {220, 260}, {400, 10}};

//size of the population
int popuSize = 1000;

//framerate
int framerate = 500;

//background colour
int backG = 255;

//text color
int textC = 0;


//another small editable bit in Wall


//-------------------------------------------------------------------------------------------------
//PREFERABLY NON-EDITABLE
//-------------------------------------------------------------------------------------------------

Population popu;
Wall walls;
Goal cGoal;

int genSinceHit = 0;

int goalSteps = 0;

public void setup() {
   //size of the window
  frameRate(framerate);//increase this to make the dots go faster
  popu = new Population(popuSize, maxSteps);//create a new population with 1000 members
  walls = new Wall();
  cGoal = new Goal();

  cGoal.setP(goals[0][0], goals[0][1]);  

}


public void draw() { 
  background(backG);
  
  fill(textC);
  text("gen: "+str(popu.gen), 10, 10, 70, 80);  // Text wraps within text box

  //draw goal
  cGoal.show();
  walls.show();

  
  if (popu.allDotsDead()) {
    
    boolean hitGoal = popu.hitGoal();
    
    //genetic algorithm
    popu.calculateFitness();
    popu.naturalSelection();
    popu.mutateDemBabies();
    
    if(hitGoal && goalSteps < (maxGoalSteps + 1)){
      genSinceHit++;
      
      //move goal
      if(genSinceHit == genToNew && goalSteps < (maxGoalSteps-1)){
        genSinceHit = 0;
        goalSteps++;
        popu.minStep = maxSteps;
        cGoal.setP(goals[goalSteps][0], goals[goalSteps][1]);
      }
    }
  } else {
    //if any of the dots are still alive then update and then show them

    popu.update();
    popu.show();
  }
}
class Brain {
  PVector[] directions;//series of vectors which get the dot to the goal (hopefully)
  int step = 0;


  Brain(int size) {
    directions = new PVector[size];
    randomize();
  }

  //--------------------------------------------------------------------------------------------------------------------------------
  //sets all the vectors in directions to a random vector with length 1
  public void randomize() {
    for (int i = 0; i< directions.length; i++) {
      float randomAngle = random(2*PI);
      directions[i] = PVector.fromAngle(randomAngle);
    }
  }

  //-------------------------------------------------------------------------------------------------------------------------------------
  //returns a perfect copy of this brain object
  public Brain clone() {
    Brain clone = new Brain(directions.length);
    for (int i = 0; i < directions.length; i++) {
      clone.directions[i] = directions[i].copy();
    }

    return clone;
  }

  //----------------------------------------------------------------------------------------------------------------------------------------

  //mutates the brain by setting some of the directions to random vectors
  public void mutate() {
    float mutationRate = 0.01f;//chance that any vector in directions gets changed
    for (int i =0; i< directions.length; i++) {
      float rand = random(1);
      if (rand < mutationRate) {
        //set this direction as a random direction 
        float randomAngle = random(2*PI);
        directions[i] = PVector.fromAngle(randomAngle);
      }
    }
  }
}
class Dot {
  PVector pos;
  PVector vel;
  PVector acc;
  Brain brain;

  boolean dead = false;
  boolean reachedGoal = false;
  boolean isBest = false;//true if this dot is the best dot from the previous generation

  float fitness = 0;

  Dot() {
    brain = new Brain(1000);//new brain with 1000 instructions

    //start the dots at the bottom of the window with a no velocity or acceleration
    pos = new PVector(width/2, height- 10);
    vel = new PVector(0, 0);
    acc = new PVector(0, 0);
  }


  //-----------------------------------------------------------------------------------------------------------------
  //draws the dot on the screen
  public void show() {
    //if this dot is the best dot from the previous generation then draw it as a big green dot
    if (isBest) {
      fill(0, 255, 0);
      ellipse(pos.x, pos.y, 8, 8);
    } else {//all other dots are just smaller black dots
      fill(0);
      ellipse(pos.x, pos.y, 4, 4);
    }
  }

  //-----------------------------------------------------------------------------------------------------------------------
  //moves the dot according to the brains directions
  public void move() {

    if (brain.directions.length > brain.step) {//if there are still directions left then set the acceleration as the next PVector in the direcitons array
      acc = brain.directions[brain.step];
      brain.step++;
    } else {//if at the end of the directions array then the dot is dead
      dead = true;
    }

    //apply the acceleration and move the dot
    vel.add(acc);
    vel.limit(5);//not too fast
    pos.add(vel);
  }

  //-------------------------------------------------------------------------------------------------------------------
  //calls the move function and check for collisions and stuff
  public void update() {
    if (!dead && !reachedGoal) {
      move();
      if (pos.x< 2|| pos.y<2 || pos.x>width-2 || pos.y>height -2) {//if near the edges of the window then kill it 
        dead = true;
      } else if (dist(pos.x, pos.y, cGoal.goal.x, cGoal.goal.y) < 5) {//if reached goal

        reachedGoal = true;
      } else if (walls.touchWall(pos)) {//if hit obstacle
        dead = true;
      }
    }
  }


  //--------------------------------------------------------------------------------------------------------------------------------------
  //calculates the fitness
  public void calculateFitness() {
    if (reachedGoal) {//if the dot reached the goal then the fitness is based on the amount of steps it took to get there
      fitness = 1.0f/16.0f + 10000.0f/(float)(brain.step * brain.step);
    } else {//if the dot didn't reach the goal then the fitness is based on how close it is to the goal
      float distanceToGoal = dist(pos.x, pos.y, cGoal.goal.x, cGoal.goal.y);
      fitness = 1.0f/(distanceToGoal * distanceToGoal);
    }
  }

  //---------------------------------------------------------------------------------------------------------------------------------------
  //clone it 
  public Dot gimmeBaby() {
    Dot baby = new Dot();
    baby.brain = brain.clone();//babies have the same brain as their parents
    return baby;
  }
}
class Goal{
  PVector goal  = new PVector(400, 10);
  Goal(){
  }
  
  public void show(){
    fill(255,0,0);
    ellipse(goal.x, goal.y, 10, 10);

  }

  public void setP(int x, int y){
    PVector wer = new PVector(x, y);
    goal.set(wer); 
  }
}
class Population {
  Dot[] dots;

  float fitnessSum;
  int gen = 1;

  int bestDot = 0;//the index of the best dot in the dots[]

  int minStep;

  Population(int size, int max) {
    dots = new Dot[size];
    for (int i = 0; i< size; i++) {
      dots[i] = new Dot();
    }
    minStep = max;
  }


  //------------------------------------------------------------------------------------------------------------------------------
  //show all dots
  public void show() {
    for (int i = 1; i< dots.length; i++) {
      dots[i].show();
    }
    dots[0].show();
  }

  //-------------------------------------------------------------------------------------------------------------------------------
  //update all dots 
  public void update() {
    for (int i = 0; i< dots.length; i++) {
      if (dots[i].brain.step > minStep) {//if the dot has already taken more steps than the best dot has taken to reach the goal
        dots[i].dead = true;//then it dead
      } else {
        dots[i].update();
      }
    }
  }

  //-----------------------------------------------------------------------------------------------------------------------------------
  //calculate all the fitnesses
  public void calculateFitness() {
    for (int i = 0; i< dots.length; i++) {
      dots[i].calculateFitness();
    }
  }


  //------------------------------------------------------------------------------------------------------------------------------------
  //returns whether all the dots are either dead or have reached the goal
  public boolean allDotsDead() {
    for (int i = 0; i< dots.length; i++) {
      if (!dots[i].dead && !dots[i].reachedGoal) { 
        return false;
      }
    }

    return true;
  }



  //-------------------------------------------------------------------------------------------------------------------------------------

  //gets the next generation of dots
  public void naturalSelection() {
    Dot[] newDots = new Dot[dots.length];//next gen
    setBestDot();
    calculateFitnessSum();

    //the champion lives on 
    newDots[0] = dots[bestDot].gimmeBaby();
    newDots[0].isBest = true;
    for (int i = 1; i< newDots.length; i++) {
      //select parent based on fitness
      Dot parent = selectParent();

      //get baby from them
      newDots[i] = parent.gimmeBaby();
    }

    dots = newDots.clone();
    gen ++;
  }


  //--------------------------------------------------------------------------------------------------------------------------------------
  //you get it
  public void calculateFitnessSum() {
    fitnessSum = 0;
    for (int i = 0; i< dots.length; i++) {
      fitnessSum += dots[i].fitness;
    }
  }

  //-------------------------------------------------------------------------------------------------------------------------------------

  //chooses dot from the population to return randomly(considering fitness)

  //this function works by randomly choosing a value between 0 and the sum of all the fitnesses
  //then go through all the dots and add their fitness to a running sum and if that sum is greater than the random value generated that dot is chosen
  //since dots with a higher fitness function add more to the running sum then they have a higher chance of being chosen
  public Dot selectParent() {
    float rand = random(fitnessSum);


    float runningSum = 0;

    for (int i = 0; i< dots.length; i++) {
      runningSum+= dots[i].fitness;
      if (runningSum > rand) {
        return dots[i];
      }
    }

    //should never get to this point

    return null;
  }

  //------------------------------------------------------------------------------------------------------------------------------------------
  //mutates all the brains of the babies
  public void mutateDemBabies() {
    for (int i = 1; i< dots.length; i++) {
      dots[i].brain.mutate();
    }
  }

  //---------------------------------------------------------------------------------------------------------------------------------------------
  //finds the dot with the highest fitness and sets it as the best dot
  public void setBestDot() {
    float max = 0;
    int maxIndex = 0;
    for (int i = 0; i< dots.length; i++) {
      if (dots[i].fitness > max) {
        max = dots[i].fitness;
        maxIndex = i;
      }
    }

    bestDot = maxIndex;
    //if this dot reached the goal then reset the minimum number of steps it takes to get to the goal
    if (dots[bestDot].reachedGoal) {
      minStep = dots[bestDot].brain.step;
    }
  }
  
  //---------------------------------------------------------------------------------------------------------------------------------------------
  
  public boolean hitGoal(){
    for(int i = 0; i < dots.length; i++){
       if(dots[i].reachedGoal){
         return true; 
       }
    }
    return false; 
  }
  
}
class Wall{
  //-------------------------------------------------------------------------------------------------
  //EDITABLE
  //-------------------------------------------------------------------------------------------------

  //amount of walls
  int maxWalls = 4;
  
  
  //-------------------------------------------------------------------------------------------------
  //PREFERABLY NON-EDITABLE
  //-------------------------------------------------------------------------------------------------

  int[][] rects = new int[maxWalls][4];
  

 Wall(){
   //-------------------------------------------------------------------------------------------------
   //EDITABLE
   //-------------------------------------------------------------------------------------------------


   //postitions of the walls
   rects[0][0] = 300;  //distance from left
   rects[0][1] = 250; //distance from top
   rects[0][2] = 600; //length
   rects[0][3] = 10;  //height
   
   rects[1][0] = 0;
   rects[1][1] = 400;
   rects[1][2] = 450;
   rects[1][3] = 10;
    
   rects[2][0] = 400;
   rects[2][1] = 600;
   rects[2][2] = 400;
   rects[2][3] = 10;
   
   rects[3][0] = 150;
   rects[3][1] = 0;
   rects[3][2] = 10;
   rects[3][3] = 300;
   //-------------------------------------------------------------------------------------------------
   //PREFERABLY NON-EDITABLE
   //-------------------------------------------------------------------------------------------------

 }
  
 public void show(){   
   fill(0, 0, 255);
   for(int i = 0; i < maxWalls; i++){
     rect(rects[i][0], rects[i][1], rects[i][2], rects[i][3]);
   }
    
 }
 
 public boolean touchWall(PVector pos){
   //outside walls
   if(pos.x< 2|| pos.y<2 || pos.x>width-2 || pos.y>height -2){
     return true; 
   } 
   //wall
   for(int i = 0; i < maxWalls; i++){
       if(pos.x < (rects[i][0]+rects[i][2]) && pos.y < (rects[i][1]+rects[i][3]) && pos.x > rects[i][0] && pos.y > rects[i][1]){
         return true;
       }
    }
   return false;
 }
 
 

}
  public void settings() {  size(800, 800); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "AIv3_1" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
