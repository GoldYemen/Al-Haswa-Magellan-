package app.alhaswamagellan.hawsa;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Beautiful animated moon phase view with:
 * - Pulsing golden glow around the moon
 * - Smooth terminator (phase) transitions
 * - Twinkling stars in background
 * - Subtle slow rotation of the star field
 * - Shadow/illumination animated appearance
 *
 * PHYSICALLY ACCURATE TERMINATOR:
 * The terminator (line between lit and dark) is the projection of a great circle
 * on the Moon's sphere. Viewed from Earth, this projects to an ellipse whose
 * semi-minor axis (x-offset) = r * cos(phase_angle), where phase_angle = 2*PI*p.
 * This produces correct crescent, quarter, gibbous, and full/new shapes.
 *
 * Northern Hemisphere convention:
 *   p=0.00 = new moon (all dark)
 *   p=0.25 = first quarter (right half lit)
 *   p=0.50 = full moon (all lit)
 *   p=0.75 = last quarter (left half lit)
 *   p=1.00 = new moon (all dark)
 *
 * Theme: dark navy #0D1B2A background, gold #D4AF37 accents, white/blue moon
 */
public class MoonPhaseView extends View
{
  // Theme colors
  private static final int COLOR_BG = 0xFF0D1B2A;
  private static final int COLOR_MOON_DARK = 0xFF1E293B;
  private static final int COLOR_MOON_LIT = 0xFFF1F5F9;
  private static final int COLOR_MOON_EDGE = 0xFF94A3B8;
  private static final int COLOR_GLOW_INNER = 0xFFD4AF37;
  private static final int COLOR_GLOW_OUTER = 0x20D4AF37;
  private static final int COLOR_STAR = 0xFFE0E1DD;
  private static final int COLOR_STAR_BRIGHT = 0xFFFFFFFF;

  // Paints
  private Paint paintBg;
  private Paint paintMoonLit;
  private Paint paintMoonDark;
  private Paint paintMoonEdge;
  private Paint paintGlow;
  private Paint paintStar;
  private Paint paintStarBright;

  // Star data
  private static class Star
  {
    float x, y;
    float radius;
    float baseAlpha;
    float phase; // for twinkling offset
  }

  private List<Star> stars = new ArrayList<>();
  private boolean starsInitialized = false;

  // Moon parameters
  private float phase = 0.0f; // 0=new, 0.5=full, 1.0=new again
  private float illumination = 0.0f; // 0-1
  private float viewCenterX, viewCenterY;
  private float moonRadius;

  // Animation state
  private float glowAlpha = 0.4f; // pulsing glow 0.2-0.8
  private float glowScale = 1.0f; // glow ring size pulse 1.0-1.15
  private float starTwinkle = 0.0f; // star twinkle phase
  private float starFieldRotation = 0.0f; // slow rotation of stars
  private float appearanceProgress = 0.0f; // 0-1 for initial fade-in
  private float targetPhase = 0.0f; // target phase for smooth transition
  private float animatedPhase = 0.0f; // current animated phase value

  // Animators
  private ValueAnimator glowPulseAnimator;
  private ValueAnimator starTwinkleAnimator;
  private ValueAnimator rotationAnimator;
  private ValueAnimator phaseTransitionAnimator;
  private ValueAnimator appearAnimator;

  public MoonPhaseView(Context context)
  {
    super(context);
    init();
  }

  public MoonPhaseView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    init();
  }

  public MoonPhaseView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init()
  {
    // Background
    paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBg.setColor(COLOR_BG);
    paintBg.setStyle(Paint.Style.FILL);

    // Moon lit side
    paintMoonLit = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintMoonLit.setColor(COLOR_MOON_LIT);
    paintMoonLit.setStyle(Paint.Style.FILL);

    // Moon dark side
    paintMoonDark = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintMoonDark.setColor(COLOR_MOON_DARK);
    paintMoonDark.setStyle(Paint.Style.FILL);

    // Moon edge highlight
    paintMoonEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintMoonEdge.setColor(COLOR_MOON_EDGE);
    paintMoonEdge.setStyle(Paint.Style.STROKE);
    paintMoonEdge.setStrokeWidth(1.5f);

    // Glow paint (radial gradient, updated per frame)
    paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintGlow.setStyle(Paint.Style.FILL);

    // Star paints
    paintStar = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintStar.setStyle(Paint.Style.FILL);

    paintStarBright = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintStarBright.setStyle(Paint.Style.FILL);

    // Start animations
    startGlowPulse();
    startStarTwinkle();
    startStarFieldRotation();
    startAppearanceAnimation();
  }

  /**
   * Set the moon phase (0-1 where 0=new, 0.25=first quarter, 0.5=full, 0.75=last quarter, 1=new)
   */
  public void setPhase(float phase, float illumination)
  {
    this.targetPhase = phase;
    this.illumination = illumination;

    // Smooth phase transition animation
    if (phaseTransitionAnimator != null && phaseTransitionAnimator.isRunning())
    {
      phaseTransitionAnimator.cancel();
    }

    float startPhase = this.animatedPhase;
    // Handle wrap-around (e.g. from 0.9 to 0.1 should go forward through 1.0)
    float diff = this.targetPhase - startPhase;
    if (Math.abs(diff) > 0.5f)
    {
      if (diff > 0)
        startPhase += 1.0f;
      else
        startPhase -= 1.0f;
    }

    phaseTransitionAnimator = ValueAnimator.ofFloat(startPhase, this.targetPhase);
    phaseTransitionAnimator.setDuration(800);
    phaseTransitionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    phaseTransitionAnimator.addUpdateListener(anim -> {
      animatedPhase = (float) anim.getAnimatedValue() % 1.0f;
      if (animatedPhase < 0)
        animatedPhase += 1.0f;
      invalidate();
    });
    phaseTransitionAnimator.start();
  }

  // ===== Animation methods =====

  private void startGlowPulse()
  {
    glowPulseAnimator = ValueAnimator.ofFloat(0.3f, 0.75f);
    glowPulseAnimator.setDuration(2500);
    glowPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
    glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
    glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    glowPulseAnimator.addUpdateListener(anim -> {
      glowAlpha = (float) anim.getAnimatedValue();
      glowScale = 1.0f + (glowAlpha - 0.3f) * 0.25f; // 1.0 to ~1.11
      invalidate();
    });
    glowPulseAnimator.start();
  }

  private void startStarTwinkle()
  {
    starTwinkleAnimator = ValueAnimator.ofFloat(0, (float) (2 * Math.PI));
    starTwinkleAnimator.setDuration(4000);
    starTwinkleAnimator.setRepeatCount(ValueAnimator.INFINITE);
    starTwinkleAnimator.setInterpolator(new LinearInterpolator());
    starTwinkleAnimator.addUpdateListener(anim -> {
      starTwinkle = (float) anim.getAnimatedValue();
      invalidate();
    });
    starTwinkleAnimator.start();
  }

  private void startStarFieldRotation()
  {
    rotationAnimator = ValueAnimator.ofFloat(0, 360);
    rotationAnimator.setDuration(120000); // Very slow: full rotation in 2 minutes
    rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
    rotationAnimator.setInterpolator(new LinearInterpolator());
    rotationAnimator.addUpdateListener(anim -> {
      starFieldRotation = (float) anim.getAnimatedValue();
      invalidate();
    });
    rotationAnimator.start();
  }

  private void startAppearanceAnimation()
  {
    appearAnimator = ValueAnimator.ofFloat(0, 1);
    appearAnimator.setDuration(600);
    appearAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    appearAnimator.addUpdateListener(anim -> {
      appearanceProgress = (float) anim.getAnimatedValue();
      invalidate();
    });
    appearAnimator.start();
  }

  // ===== Drawing =====

  private void initStars(int w, int h)
  {
    if (starsInitialized)
      return;
    starsInitialized = true;

    Random rand = new Random(42); // Fixed seed for consistent star positions
    stars.clear();

    int starCount = 80;
    for (int i = 0; i < starCount; i++)
    {
      Star s = new Star();
      s.x = rand.nextFloat() * w;
      s.y = rand.nextFloat() * h;
      s.radius = rand.nextFloat() * 1.5f + 0.5f;
      s.baseAlpha = rand.nextFloat() * 0.5f + 0.2f;
      s.phase = rand.nextFloat() * (float) (2 * Math.PI);
      stars.add(s);
    }
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    super.onDraw(canvas);

    int w = getWidth();
    int h = getHeight();
    initStars(w, h);

    viewCenterX = w / 2f;
    viewCenterY = h / 2f;
    moonRadius = Math.min(w, h) * 0.22f;

    // 1) Background
    canvas.drawRect(0, 0, w, h, paintBg);

    // 2) Rotated star field
    canvas.save();
    canvas.rotate(starFieldRotation, viewCenterX, viewCenterY);
    drawStars(canvas, w, h);
    canvas.restore();

    // 3) Moon glow (behind moon)
    if (appearanceProgress > 0.3f)
    {
      drawMoonGlow(canvas);
    }

    // 4) Moon body
    if (appearanceProgress > 0.5f)
    {
      drawMoonBody(canvas);
    }
  }

  private void drawStars(Canvas canvas, int w, int h)
  {
    for (Star s : stars)
    {
      // Twinkle: alpha varies sinusoidally
      float twinkleAlpha = s.baseAlpha + 0.3f * (float) Math.sin(starTwinkle + s.phase);
      twinkleAlpha = Math.max(0.05f, Math.min(1.0f, twinkleAlpha));
      twinkleAlpha *= appearanceProgress;

      // Occasional bright flash
      boolean bright = (Math.sin(starTwinkle * 2 + s.phase * 3) > 0.85);

      if (bright)
      {
        paintStarBright.setColor(COLOR_STAR_BRIGHT);
        paintStarBright.setAlpha((int) (twinkleAlpha * 255));
        canvas.drawCircle(s.x, s.y, s.radius * 1.5f, paintStarBright);
      }
      else
      {
        paintStar.setColor(COLOR_STAR);
        paintStar.setAlpha((int) (twinkleAlpha * 255));
        canvas.drawCircle(s.x, s.y, s.radius, paintStar);
      }
    }
  }

  private void drawMoonGlow(Canvas canvas)
  {
    float glowRadius = moonRadius * 1.6f * glowScale;
    float effectiveAlpha = glowAlpha * appearanceProgress;

    // Outer glow
    RadialGradient glowGrad = new RadialGradient(
        viewCenterX, viewCenterY, glowRadius,
        new int[] {Color.argb((int) (effectiveAlpha * 180), 0xD4, 0xAF, 0x37),
                   Color.argb((int) (effectiveAlpha * 60), 0xD4, 0xAF, 0x37), Color.argb(0, 0xD4, 0xAF, 0x37)},
        new float[] {0.3f, 0.65f, 1.0f}, Shader.TileMode.CLAMP);

    paintGlow.setShader(glowGrad);
    paintGlow.setAlpha((int) (effectiveAlpha * 255));
    canvas.drawCircle(viewCenterX, viewCenterY, glowRadius, paintGlow);
  }

  private void drawMoonBody(Canvas canvas)
  {
    float scale = appearanceProgress;
    float effectiveRadius = moonRadius * scale;

    // Full moon circle (lit side base)
    paintMoonLit.setAlpha((int) (255 * scale));
    canvas.drawCircle(viewCenterX, viewCenterY, effectiveRadius, paintMoonLit);

    // Draw the dark side overlay using terminator
    drawTerminator(canvas, effectiveRadius, scale);

    // Moon edge ring
    paintMoonEdge.setAlpha((int) (180 * scale));
    canvas.drawCircle(viewCenterX, viewCenterY, effectiveRadius, paintMoonEdge);

    // Surface detail (subtle craters)
    if (scale > 0.7f)
    {
      drawCraters(canvas, effectiveRadius);
    }
  }

  /**
   * Draw the dark (shadow) overlay using the physically accurate terminator model.
   *
   * The terminator is the projection of the great circle dividing the Moon's
   * lit and dark hemispheres. When projected onto the 2D disk, it forms an
   * ellipse whose x-offset (semi-minor axis) = r * cos(phase_angle),
   * where phase_angle = 2 * PI * p.
   *
   * This produces correct shapes for all phases:
   *   New (p=0):     terminX = +r  → all dark
   *   1st Qtr (p=.25): terminX = 0  → right half lit
   *   Full (p=.5):   terminX = -r → all lit
   *   3rd Qtr (p=.75): terminX = 0  → left half lit
   *   New (p=1):     terminX = +r  → all dark
   *
   * Northern Hemisphere convention: waxing = right side lit, waning = left side lit.
   */
  private void drawTerminator(Canvas canvas, float radius, float scale)
  {
    float p = animatedPhase;

    // Compute illumination from phase (for edge case handling)
    double illum = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * p));

    if (illum >= 0.99)
    {
      // Full moon — all lit, no dark overlay
      return;
    }
    else if (illum <= 0.01)
    {
      // New moon — all dark
      paintMoonDark.setAlpha((int) (255 * scale));
      canvas.drawCircle(viewCenterX, viewCenterY, radius, paintMoonDark);
      return;
    }

    // Physically accurate terminator position
    // The terminator is the projection of the great circle dividing lit and dark hemispheres.
    // For waxing phases (p <= 0.5), the visible terminator semi-ellipse is at
    //   x = cx + r*cos(2PIp)*cos(theta)
    // For waning phases (p > 0.5), the visible terminator is on the opposite side:
    //   x = cx - r*cos(2PIp)*cos(theta)  (i.e., the sign flips)
    // This ensures correct crescent/gibbous shapes for all phases.
    boolean isWaxing = (p <= 0.5f);
    double signedTerminatorX = (isWaxing ? 1.0 : -1.0) * radius * Math.cos(2.0 * Math.PI * p);

    // Build the dark (shadow) path
    Path darkPath = new Path();

    // Start at top of moon disk
    darkPath.moveTo(viewCenterX, viewCenterY - radius);

    // Draw the dark limb arc (the outer edge on the dark side)
    if (isWaxing)
    {
      // Waxing: right side is lit, dark limb is LEFT side
      // Left limb: arc from top (-90°) to bottom (+90°) going counter-clockwise (-180° sweep)
      darkPath.arcTo(viewCenterX - radius, viewCenterY - radius, viewCenterX + radius, viewCenterY + radius, -90, -180,
                     true);
    }
    else
    {
      // Waning: left side is lit, dark limb is RIGHT side
      // Right limb: arc from top (-90°) to bottom (+90°) going clockwise (+180° sweep)
      darkPath.arcTo(viewCenterX - radius, viewCenterY - radius, viewCenterX + radius, viewCenterY + radius, -90, 180,
                     false);
    }

    // Walk along the terminator from bottom back to top
    // The terminator is an ellipse: x = cx + terminatorX * cos(angle), y = cy + r * sin(angle)
    // angle goes from +90° (bottom) to -90° (top)
    int steps = 40;
    for (int i = steps; i >= 0; i--)
    {
      float angle = -90f + (180f * i / steps);
      float y = viewCenterY + radius * (float) Math.sin(Math.toRadians(angle));
      float xTerminator = viewCenterX + (float) (signedTerminatorX * Math.cos(Math.toRadians(angle)));
      darkPath.lineTo(xTerminator, y);
    }

    darkPath.close();

    paintMoonDark.setAlpha((int) (230 * scale));
    canvas.drawPath(darkPath, paintMoonDark);
  }

  private void drawCraters(Canvas canvas, float radius)
  {
    // Subtle surface detail — a few small darker circles
    Paint craterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    craterPaint.setStyle(Paint.Style.FILL);
    craterPaint.setColor(0xFFCBD5E1); // slightly darker than moon lit
    craterPaint.setAlpha(35);

    float r = radius;
    // Some fixed crater positions relative to moon center
    canvas.drawCircle(viewCenterX + r * 0.3f, viewCenterY - r * 0.2f, r * 0.06f, craterPaint);
    canvas.drawCircle(viewCenterX - r * 0.15f, viewCenterY + r * 0.25f, r * 0.05f, craterPaint);
    canvas.drawCircle(viewCenterX + r * 0.1f, viewCenterY + r * 0.1f, r * 0.07f, craterPaint);
    canvas.drawCircle(viewCenterX - r * 0.3f, viewCenterY - r * 0.15f, r * 0.04f, craterPaint);
    canvas.drawCircle(viewCenterX + r * 0.35f, viewCenterY + r * 0.3f, r * 0.05f, craterPaint);
  }

  // ===== Lifecycle =====

  @Override
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    stopAllAnimations();
  }

  public void stopAllAnimations()
  {
    if (glowPulseAnimator != null)
      glowPulseAnimator.cancel();
    if (starTwinkleAnimator != null)
      starTwinkleAnimator.cancel();
    if (rotationAnimator != null)
      rotationAnimator.cancel();
    if (phaseTransitionAnimator != null)
      phaseTransitionAnimator.cancel();
    if (appearAnimator != null)
      appearAnimator.cancel();
  }

  public void startAllAnimations()
  {
    if (glowPulseAnimator == null || !glowPulseAnimator.isRunning())
      startGlowPulse();
    if (starTwinkleAnimator == null || !starTwinkleAnimator.isRunning())
      startStarTwinkle();
    if (rotationAnimator == null || !rotationAnimator.isRunning())
      startStarFieldRotation();
  }
}
