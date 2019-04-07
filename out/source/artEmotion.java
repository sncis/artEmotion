import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.Iterator; 
import org.openkinect.processing.*; 
import themidibus.*; 
import java.util.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class artEmotion extends PApplet {





Kinect kinect;
// MidiBus startBus = new MidiBus(this, "startBus", "startBus");
// MidiBus busDavid = new MidiBus(this, "busDavid", "busDavid");


final int PARTILE_MAX_VEL = 20; ///7;//4;
final int PARTICLE_MAX_ACC = 10; // Max particle acceleration
final int SPAWN_COUNT = 2; // Number of particles to spawn at once
final int START_SIZE = 30;//100;//175;
final int MAX_PARTICLES = 100;
final int SPAWN_DELAY = 50; //ms
final float LIFESPAN_DECREMENT = 2.0f;
final float SHRINK_RATE = 1;//2;//5;
final float MIN_THRESH = 600;
final float MAX_THRESH = 760;

int animation2Iterations = 40;
int ellapsedTime = millis();

float sumX = 0;
float sumY = 0;
float totalPixels = 0;
PVector lastAvgPos = new PVector(0, 0);

boolean isPushing = false;
boolean shouldRenderAnimation1 = false;

Attractor hand;

ParticleSystem system = new ParticleSystem();
ColourGenerator colour = new ColourGenerator();
ParticleSystem2 particleSystem = new ParticleSystem2();

public void setup() {
  
  kinect = new Kinect(this);
  kinect.initDepth();
  // kinect.enableMirror(true);
  background(0);
  frameRate(30);
  particleSystem.addParticle();

  // startBus.sendControllerChange(1,122,120); // starts the first clip(baseLine)
  // startBus.sendControllerChange(1,7,127);

}

public void draw() {
  int[] depth = kinect.getRawDepth();
  
  if (shouldRenderAnimation1 == true) {
    renderAnimation1(depth);
  } else {
    renderAnimation2(depth);
  } 
}
class Attractor{
  Particle2 particle;
  float mass;
  PVector location;
  float g;
  float strength;
  float distance;

  Attractor(PVector position){
    location = position.get();
    mass = random(20); 
    g = 5;
  }

  public PVector attract(Particle2 particle){
    PVector force = PVector.sub(location, particle.position);
    distance = force.mag();
    force.normalize();
    strength = g / distance * distance;
    force.mult(strength);
    colour.update();
    
    return force;
  }

}
// Killeroo (2018), Processing (Version 3) https://www.openprocessing.org/sketch/504589
class ColourGenerator {
  final static float MIN_SPEED = 0.2f;
  final static float MAX_SPEED = 0.7f;
  float R, G, B;
  float Rspeed, Gspeed, Bspeed;
  
  ColourGenerator() {
    init();  
  }
  
  public void init() {
    // Initialize colours
    R = random(255);
    G = random(255);
    B = random(255);
    
    // Starting transition speed
    Rspeed = (random(1) > 0.5f ? 1 : -1) * random(MIN_SPEED, MAX_SPEED);
    Gspeed = (random(1) > 0.5f ? 1 : -1) * random(MIN_SPEED, MAX_SPEED);
    Bspeed = (random(1) > 0.5f ? 1 : -1) * random(MIN_SPEED, MAX_SPEED);
  }
  
  public void update() {
    // Use transition to alter original colour (Keep within RGB bounds)
    Rspeed = ((R += Rspeed) > 255 || (R < 0)) ? -Rspeed : Rspeed;
    Gspeed = ((G += Gspeed) > 255 || (G < 0)) ? -Gspeed : Gspeed;
    Bspeed = ((B += Bspeed) > 255 || (B < 0)) ? -Bspeed : Bspeed;
  }
}
public void renderAnimation1(int[] depth) {
  // Update the particle system with each iteration
  drawBackground();
  system.update();
  pixelIterator(depth);
}

public void renderAnimation2(int[] depth) {
  background(0);
  particleSystem.run();

  float avgX = 0;
  float avgY = 0;

  sumX = 0;
  sumY = 0;
  totalPixels = 0;
  
  pixelIterator(depth);

  avgX = sumX / totalPixels;
  avgY = sumY / totalPixels;
  PVector avgPosition = new PVector(avgX, avgY);
  if(animation2Iterations == 40) {
    if (lastAvgPos.x < avgPosition.x + 20 && lastAvgPos.x > avgPosition.x - 20 && lastAvgPos.y < avgPosition.y + 20 && lastAvgPos.y > avgPosition.y - 20) {
      shouldRenderAnimation1 = true;
      system.particles.clear();
    }

    lastAvgPos = avgPosition;
    animation2Iterations = 0;
  }

  animation2Iterations += 1;

  if(avgPosition.x > 0 && avgPosition.y > 0) {
    particleSystem.getAttracted(avgPosition);
  }
}

public void pixelIterator(int[] depth) {
  for(int x = 0; x < kinect.width; x++){
    for(int y = 0; y < kinect.height; y++){
      // Iterates over each pixel and gets its depth value
      int offset = x + y * kinect.width;
      int d = depth[offset];

      // If the depth value is between a certain threshold...
      if(d > MIN_THRESH && d < MAX_THRESH) {
        if (shouldRenderAnimation1 == true) {
          addParticlesAnimation1(d, x, y);
        } else {
          sumX += x;
          sumY += y;
          totalPixels ++;
        }
      // if not, we probably ended a push so we update the variable isPushing
      } else {
        if(millis() > ellapsedTime + 100) {
          isPushing = false;
          system.clearCount();
          // busDavid.sendControllerChange(1,123,0);
        }
      }
    }
  }
}

// Gives us the fading away background
public void drawBackground() {
  // noStroke();
  fill(0, 20);
  rect(0, 0, width, height);
}

public void addParticlesAnimation1(int d, int x, int y) {
  // Add particles to random locations around the position where the user pushed
  ellapsedTime = millis();
  if(isPushing == false) {
    isPushing = true;
    for(int i = 0; i < 24; i++) {
      system.addParticle(new PVector(random(x-50, x+50), random(y-50, y+50)));
    }
  }
}

public void mouseDragged(){
  // addParticlesFirstScreen(int(random(100)),mouseX, mouseY);
  // system.addParticle(new PVector(mouseX, mouseY));
  // system.update();

  PVector mouse = new PVector(mouseX, mouseY);
  particleSystem.getAttracted(mouse);
}

public void keyPressed(){
  if(key == 'A' || key =='a'){
    //startBus.sendControllerChange(1,7,0);
    exit();
  }
}
class Particle {
  PVector loc;
  PVector vel;
  PVector acc;

  int size = START_SIZE;
  float angle;
  float lifespan;
  
  Particle(PVector pos) {
    loc = new PVector(pos.x, pos.y);
    vel = new PVector(0, 0);
    acc = new PVector(0, 0);
    lifespan = 255.0f;
  }
  
  public void update() {
    // Calculate the direction the particle is going to turn
    angle = random(0, TWO_PI);
    
    // Cos and Sin are complementary and setting the x and y values gives us the beautiful smooth turns. 
    acc.x += cos(angle) * 5;
    acc.y += sin(angle) * 5;
    
    // Limit result
    acc.limit(PARTICLE_MAX_ACC);
    
    // Add acceleration to current velocity and then limit result
    vel.add(acc);
    vel.limit(PARTILE_MAX_VEL);
    
    // Appy velocity to current location
    loc.add(vel);

    // Decrease the lifespan slowly over time.
    lifespan -= 2.0f;
    
    // Wrap around the screen, https://processing.org/tutorials/pvector/
    if (loc.x > width)
      loc.x -= width;
     if (loc.x < 0)
       loc.x += width;
     if(loc.y > height)
       loc.y -= height;
     if(loc.y < 0)
       loc.y += height;
    
    // If the user stopped pushing, start shrinking the particles.
    if(isPushing == false) {
      size -= SHRINK_RATE;
    }

    // If the lifespan reaches 0, we move to the enxt animation.
    if(lifespan <= 0) {
      shouldRenderAnimation1 = false;
      animation2Iterations = 40;
    }
  }
  
  public void display() {
    colour.update();
    fill(colour.R, colour.G, colour.B, lifespan);
    ellipse(loc.x, loc.y, size, size);
  }
  
  public boolean isDead() {
    if (size < 0) {
      return true;
    } else {
      return false;
    }
  }
}
class Particle2{
  PVector position;
  PVector velocity;
  PVector acceleration;
  float mass;
  PVector force;

  Particle2(){
    mass = random(10);
    position = new PVector(random(0, width), random(0, height));
    velocity = new PVector(0, 0);
    acceleration = new PVector(random(-1,1), random(-1,1));
  }
  
  public void applyForce(PVector f){
    PVector force = PVector.div(f, mass);
    acceleration.add(force);
  }

  public PVector repulse(Particle2 part){
    float g = 5;
    force = PVector.sub(position, part.position);
    float distance = force.mag();

    force.normalize();
    float strength = (g * mass * part.mass) / distance;
    force.mult(strength);
    return force;
  }

  public void display(){
    fill(colour.R, colour.G, colour.B);
    ellipse(position.x, position.y, mass * 2, mass * 2);
  }

  public void move(){
    velocity.add(acceleration);
    velocity.limit(4);
    position.add(velocity);
    acceleration.mult(0);
  }

  public void checkEdges(){
    if(position.x < 0){
      velocity.x *= -1;
    }
    if(position.x > width){
      velocity.x *= -1;
    }
    if(position.y < 0){
      velocity.y *= -1;
    }
    if(position.y > height){
      velocity.y *= -1;
    }
  }

  public void run(){
    checkEdges();
    move();
    display();
  }
}
class ParticleSystem {
  // Initialize our particles array list
  ArrayList<Particle> particles = new ArrayList<Particle>();

  // Counter to keep track of the amount of particles on the screen
  int count = 0;

  public void clearCount() {
    count = 0;
  }
    
  // Adds a new particle if there are less than 24 particles on the screen.
  public void addParticle(PVector loc) {
    if (count <= 24) {
      count++;
      particles.add(new Particle(loc));
    }
  }

  public void update() {
    // Use an iterator to loop through active particles
    Iterator<Particle> i = particles.iterator();
    
    while(i.hasNext()) {
      // Get next particle
      Particle p = i.next();
      
      //sending notes depending on particle position to ableton
      // int note = int(map(p.loc.x, 0, width, 0,127));
      // int velocity = int(map(p.loc.y, 0, height, 127, 0));
      // busDavid.sendNoteOn(1, note, velocity);
      
      // update position and lifespan
      p.update();
      // Remove particle if dead
      if (p.isDead()) {
        i.remove();
        count--;
      } else {
        p.display();
      }
    }
  }
}


class ParticleSystem2{
  // MidiBus attractBus = new MidiBus(this, "attractBus", "attractBus");

  Particle2 particle;
  ArrayList<Particle2> particleList = new ArrayList<Particle2>();
  Attractor hand;
  PVector position;
  PVector force;
  int particleNumber = 500;

  
  public void addParticle(){
    for(int i = 0 ; i < particleNumber; i++){
      particle = new Particle2();
      particleList.add(particle);
    }
  }

  public void showParticle(){
    for(Particle2 part : particleList){
      part.run();
    }
  }

  public void repulseParticle(){
    for(int i = 0; i < particleList.size(); i++){
      for(int j = 0; j< particleList.size(); j++){
        if(i != j){
          Particle2 p1 = particleList.get(i);
          Particle2 p2 = particleList.get(j);
          float distance = dist(p1.position.x, p1.position.y, p2.position.x, p2.position.y);

          if(distance < 10){
            force = p1.repulse(p2);
            p1.applyForce(force);
          }
        }
      }
    }
  }

  public void getAttracted(PVector location){
    // int pitch = int(map(location.y, 0, height, 127,64));
    // int velocity = (int(map(location.y,0, height, 127,64)));
    // attractBus.sendNoteOn(1, pitch, velocity);
    
    hand = new Attractor(location);

    for(Particle2 part : particleList){
      force = hand.attract(part);
      part.applyForce(force);
    }
  }

  public void run(){
    repulseParticle();
    showParticle();
  }
}
  public void settings() {  size(640, 480); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "artEmotion" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
