package com.urbandroid.sleep.captcha.examples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.urbandroid.sleep.captcha.CaptchaSupport;
import com.urbandroid.sleep.captcha.CaptchaSupportFactory;

import java.util.Locale;
import java.util.Random;

public class HardMathCaptchaActivity extends Activity {

    private static final long MAX_ANSWER_ABS = 9_999_999_999L;
    private static final int MAX_GENERATION_TRIES = 30;

    private CaptchaSupport captchaSupport;

    private TextView questionView;
    private EditText answerInput;
    private Button checkButton;

    private long correctAnswer;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_math_captcha);

        captchaSupport = CaptchaSupportFactory.create(this);

        questionView = findViewById(R.id.question);
        answerInput = findViewById(R.id.answer_input);
        checkButton = findViewById(R.id.check_button);

        generateNewProblem();

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswer();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        captchaSupport = CaptchaSupportFactory.create(this, intent);
        generateNewProblem();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captchaSupport != null) {
            captchaSupport.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (captchaSupport != null) {
            captchaSupport.unsolved();
        }
        super.onBackPressed();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (captchaSupport != null) {
            captchaSupport.alive();
        }
    }

    private void generateNewProblem() {
        int difficulty = 1;
        if (captchaSupport != null) {
            try {
                difficulty = captchaSupport.getDifficulty(); // 1..5
            } catch (Throwable ignored) {
                difficulty = 1;
            }
        }

        Problem p = null;
        for (int i = 0; i < MAX_GENERATION_TRIES; i++) {
            Problem candidate = createProblemForDifficulty(difficulty);

            if (candidate == null) continue;
            if (Math.abs(candidate.answer) > MAX_ANSWER_ABS) continue;
            if (candidate.answer == Long.MIN_VALUE || candidate.answer == Long.MAX_VALUE) continue;

            p = candidate;
            break;
        }

        if (p == null) {
            p = new Problem("12 × 12 + 34", 178);
        }

        correctAnswer = p.answer;
        questionView.setText(p.text);
        answerInput.setText("");
    }

    private void checkAnswer() {
        if (captchaSupport != null) {
            captchaSupport.alive();
        }

        String text = answerInput.getText() == null ? "" : answerInput.getText().toString().trim();
        if (text.isEmpty()) {
            answerInput.setError(getString(R.string.err_enter_answer));
            return;
        }

        long userAnswer;
        try {
            userAnswer = Long.parseLong(text);
        } catch (NumberFormatException e) {
            answerInput.setError(getString(R.string.err_integers_only));
            return;
        }

        if (userAnswer == correctAnswer) {
            Toast.makeText(this, getString(R.string.ok_correct), Toast.LENGTH_SHORT).show();
            if (captchaSupport != null) {
                captchaSupport.solved();
            }
            finish();
        } else {
            answerInput.setError(getString(R.string.err_wrong_new));
            generateNewProblem();
        }
    }

    private Problem createProblemForDifficulty(int difficulty) {
        if (difficulty <= 1) {
            return createEasyProblem();
        } else if (difficulty == 2) {
            return createMediumProblem();
        } else if (difficulty == 3) {
            return createHardProblem();
        } else {
            return createInsaneProblem(difficulty);
        }
    }

    // ---------------------------
    // Easy / Medium / Hard
    // ---------------------------

    // a (+|-) b
    private Problem createEasyProblem() {
        int a = random.nextInt(200) + 1; // 1..200
        int b = random.nextInt(200) + 1; // 1..200
        boolean plus = random.nextBoolean();

        long ans = plus ? (a + b) : Math.max(0, a - b);
        String text = a + (plus ? " + " : " - ") + b;
        return new Problem(text, ans);
    }

    // (a * b) (+|-) c
    private Problem createMediumProblem() {
        int a = random.nextInt(40) + 2; // 2..41
        int b = random.nextInt(40) + 2; // 2..41
        int c = random.nextInt(400) + 1; // 1..400

        long mul = (long) a * (long) b;
        boolean plus = random.nextBoolean();

        long ans = plus ? (mul + c) : Math.max(0, mul - c);
        String text = "(" + a + " × " + b + ")" + (plus ? " + " : " - ") + c;
        return new Problem(text, ans);
    }

    // (a^b * c) (+|-) d
    private Problem createHardProblem() {
        int a = random.nextInt(7) + 2;   // 2..8
        int b = random.nextInt(4) + 2;   // 2..5
        int c = random.nextInt(80) + 2;  // 2..81
        int d = random.nextInt(1500) + 1;// 1..1500

        long p = powChecked(a, b);
        if (p == Long.MAX_VALUE) return null;

        long mul = mulChecked(p, c);
        if (mul == Long.MAX_VALUE) return null;

        boolean plus = random.nextBoolean();
        long ans = plus ? addChecked(mul, d) : Math.max(0, subChecked(mul, d));
        if (ans == Long.MAX_VALUE) return null;

        String text = "(" + a + "^" + b + " × " + c + ")" + (plus ? " + " : " - ") + d;
        return new Problem(text, ans);
    }

    // ---------------------------
    // Insane (difficulty 4..5)
    // ---------------------------

    private Problem createInsaneProblem(int level) {
        int typeCount = (level >= 5) ? 8 : 5;
        int type = random.nextInt(typeCount);

        switch (type) {
            case 0:
                return insane0(level); // a! + b^c × d
            case 1:
                return insane1(level); // (a^b + c^d) × e
            case 2:
                return insane2(level); // (a! × b) % m + n
            case 3:
                return insane3(level); // log_b(a^c × d!) + e
            case 4:
                return insane4(level); // (a^b % m) × (c! + d)
            case 5:
                return insane5(level); // (a! + b!) × (c^d) % m
            case 6:
                return insane6(level); // (a^b + c!) % m + n^k
            case 7:
                return insane7(level); // (a! × b^c) - (d × e) (clamped)
            default:
                return insane0(level);
        }
    }

    // a! + b^c × d
    private Problem insane0(int level) {
        int a = randRange(4, level >= 5 ? 10 : 9);     // factorial 4..9/10
        int b = randRange(2, 8);
        int c = randRange(2, 5);
        int d = randRange(2, 80);

        long fa = factChecked(a);
        long p = powChecked(b, c);
        long mul = mulChecked(p, d);
        long ans = addChecked(fa, mul);

        if (fa == Long.MAX_VALUE || p == Long.MAX_VALUE || mul == Long.MAX_VALUE || ans == Long.MAX_VALUE) return null;

        String text = a + "! + " + b + "^" + c + " × " + d;
        return new Problem(text, ans);
    }

    // (a^b + c^d) × e
    private Problem insane1(int level) {
        int a = randRange(2, 8);
        int b = randRange(2, 5);
        int c = randRange(2, 8);
        int d = randRange(2, 5);
        int e = randRange(2, level >= 5 ? 40 : 25);

        long p1 = powChecked(a, b);
        long p2 = powChecked(c, d);
        long sum = addChecked(p1, p2);
        long ans = mulChecked(sum, e);

        if (p1 == Long.MAX_VALUE || p2 == Long.MAX_VALUE || sum == Long.MAX_VALUE || ans == Long.MAX_VALUE) return null;

        String text = "(" + a + "^" + b + " + " + c + "^" + d + ") × " + e;
        return new Problem(text, ans);
    }

    // (a! × b) % m + n
    private Problem insane2(int level) {
        int a = randRange(4, level >= 5 ? 10 : 9);
        int b = randRange(2, 120);
        int m = randRange(25, level >= 5 ? 200 : 120);
        int n = randRange(10, 500);

        long fa = factChecked(a);
        long mul = mulChecked(fa, b);
        if (fa == Long.MAX_VALUE || mul == Long.MAX_VALUE) return null;

        long mod = positiveMod(mul, m);
        long ans = addChecked(mod, n);
        if (ans == Long.MAX_VALUE) return null;

        String text = "(" + a + "! × " + b + " % " + m + ") + " + n;
        return new Problem(text, ans);
    }

    // log_b(a^c × d!) + e  (целочисленный log, floor)
    private Problem insane3(int level) {
        int base = randRange(2, 5); // log base 2..5
        int a = randRange(2, 9);
        int c = randRange(2, 6);
        int d = randRange(4, level >= 5 ? 10 : 9);
        int e = randRange(10, 1500);

        long p = powChecked(a, c);
        long fd = factChecked(d);
        long mul = mulChecked(p, fd);

        if (p == Long.MAX_VALUE || fd == Long.MAX_VALUE || mul == Long.MAX_VALUE) return null;

        int lg = floorLog(base, mul);
        long ans = addChecked(lg, e);
        if (ans == Long.MAX_VALUE) return null;

        String text = String.format(Locale.US, "log_%d(%d^%d × %d!) + %d", base, a, c, d, e);
        return new Problem(text, ans);
    }

    // (a^b % m) × (c! + d)
    private Problem insane4(int level) {
        int a = randRange(2, 10);
        int b = randRange(2, 7);
        int m = randRange(20, level >= 5 ? 250 : 150);
        int c = randRange(4, level >= 5 ? 10 : 9);
        int d = randRange(10, 500);

        long p = powChecked(a, b);
        long left = positiveMod(p, m);

        long fc = factChecked(c);
        long right = addChecked(fc, d);

        long ans = mulChecked(left, right);

        if (p == Long.MAX_VALUE || fc == Long.MAX_VALUE || right == Long.MAX_VALUE || ans == Long.MAX_VALUE) return null;

        String text = "(" + a + "^" + b + " % " + m + ") × (" + c + "! + " + d + ")";
        return new Problem(text, ans);
    }

    // (a! + b!) × (c^d) % m
    private Problem insane5(int level) {
        int a = randRange(4, level >= 5 ? 10 : 9);
        int b = randRange(4, level >= 5 ? 10 : 9);
        int c = randRange(2, 8);
        int d = randRange(2, 5);
        int m = randRange(50, level >= 5 ? 400 : 250);

        long fa = factChecked(a);
        long fb = factChecked(b);
        long sum = addChecked(fa, fb);

        long p = powChecked(c, d);
        long mul = mulChecked(sum, p);

        if (fa == Long.MAX_VALUE || fb == Long.MAX_VALUE || sum == Long.MAX_VALUE || p == Long.MAX_VALUE || mul == Long.MAX_VALUE) return null;

        long ans = positiveMod(mul, m);

        String text = "(" + a + "! + " + b + "!) × (" + c + "^" + d + ") % " + m;
        return new Problem(text, ans);
    }

    // (a^b + c!) % m + n^k
    private Problem insane6(int level) {
        int a = randRange(2, 10);
        int b = randRange(2, 6);
        int c = randRange(4, level >= 5 ? 10 : 9);
        int m = randRange(50, level >= 5 ? 400 : 250);
        int n = randRange(2, 12);
        int k = randRange(2, 4);

        long p = powChecked(a, b);
        long fc = factChecked(c);
        long sum = addChecked(p, fc);

        if (p == Long.MAX_VALUE || fc == Long.MAX_VALUE || sum == Long.MAX_VALUE) return null;

        long left = positiveMod(sum, m);

        long right = powChecked(n, k);
        if (right == Long.MAX_VALUE) return null;

        long ans = addChecked(left, right);
        if (ans == Long.MAX_VALUE) return null;

        String text = "(" + a + "^" + b + " + " + c + "!) % " + m + " + " + n + "^" + k;
        return new Problem(text, ans);
    }

    // (a! × b^c) - (d × e) with clamp to non-negative
    private Problem insane7(int level) {
        int a = randRange(4, level >= 5 ? 10 : 9);
        int b = randRange(2, 8);
        int c = randRange(2, 5);
        int d = randRange(10, 150);
        int e = randRange(10, 150);

        long fa = factChecked(a);
        long p = powChecked(b, c);
        long left = mulChecked(fa, p);

        long right = mulChecked(d, e);

        if (fa == Long.MAX_VALUE || p == Long.MAX_VALUE || left == Long.MAX_VALUE || right == Long.MAX_VALUE) return null;

        long raw = subChecked(left, right);
        if (raw == Long.MAX_VALUE) return null;

        long ans = Math.max(0, raw);

        String text = "(" + a + "! × " + b + "^" + c + ") - (" + d + " × " + e + ")";
        return new Problem(text, ans);
    }

    // ---------------------------
    // Math helpers (overflow-safe)
    // ---------------------------

    private int randRange(int minIncl, int maxIncl) {
        if (maxIncl <= minIncl) return minIncl;
        return random.nextInt(maxIncl - minIncl + 1) + minIncl;
    }

    private long addChecked(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }

    private long subChecked(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }

    private long mulChecked(long x, long y) {
        if (x == 0 || y == 0) return 0;
        if (x > 0 && y > 0 && x > Long.MAX_VALUE / y) return Long.MAX_VALUE;
        if (x > 0 && y < 0 && y < Long.MIN_VALUE / x) return Long.MAX_VALUE;
        if (x < 0 && y > 0 && x < Long.MIN_VALUE / y) return Long.MAX_VALUE;
        if (x < 0 && y < 0 && x < Long.MAX_VALUE / y) return Long.MAX_VALUE;
        return x * y;
    }

    private long powChecked(int base, int exp) {
        if (exp < 0) return Long.MAX_VALUE;
        long res = 1L;
        long b = base;
        for (int i = 0; i < exp; i++) {
            res = mulChecked(res, b);
            if (res == Long.MAX_VALUE) return Long.MAX_VALUE;
        }
        return res;
    }

    private long factChecked(int n) {
        if (n < 0) return Long.MAX_VALUE;
        long res = 1L;
        for (int i = 2; i <= n; i++) {
            res = mulChecked(res, i);
            if (res == Long.MAX_VALUE) return Long.MAX_VALUE;
        }
        return res;
    }

    private long positiveMod(long a, long m) {
        if (m <= 0) return 0;
        long r = a % m;
        if (r < 0) r += m;
        return r;
    }

    // floor(log_base(n)) for base>=2, n>=1
    private int floorLog(int base, long n) {
        if (base < 2 || n < 1) return 0;
        int e = 0;
        long x = n;
        while (x >= base) {
            x /= base;
            e++;
            if (e > 60) break;
        }
        return e;
    }

    // ---------------------------
    // Data class
    // ---------------------------

    private static class Problem {
        final String text;
        final long answer;

        Problem(String text, long answer) {
            this.text = text;
            this.answer = answer;
        }
    }
}
