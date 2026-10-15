package com.example.slova;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    GameView game;
    @Override public void onCreate(Bundle b) { super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(245,247,250)); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); game = new GameView(this); setContentView(game); }
    @Override public void onBackPressed() { if (game.selected.size() > 0) { game.selected.clear(); game.invalidate(); } else super.onBackPressed(); }

    static final String[] BASE = {"ЛИСТОК", "КОРОВКА", "КАПУСТА", "КАРТИНА", "РОМАШКА", "СНЕГОВИК", "ПОДАРОК", "КАРУСЕЛЬ"};
    static final String[][] WORDS = {
        {"ЛИСТ", "СТОЛ", "СТОК", "СКОТ", "СИТО", "ЛОСК", "ЛИКО", "КОТ", "ТОК", "СОК", "КИТ", "ЛИС"},
        {"КОРА", "КОРКА", "КОВКА", "КОРОВА", "РОК", "РАК", "ВОР", "ВАР", "КОВ", "ОКО"},
        {"КАПУСТА", "ПАСТА", "ПАУК", "СКАТ", "КУСТ", "СТУК", "ПУСК", "КАПА", "АКТ", "СУП", "ТУК"},
        {"КАРТА", "НИТКА", "ТКАНИ", "РАНА", "РИТМ", "ТАНК", "КРАН", "КАНТ", "ТИР", "КИТ", "РАК", "АКТ"},
        {"КОМАР", "КОШАРА", "МАРШ", "РАМА", "ШАР", "МАК", "РАК", "КОШ", "РОМ", "КОРА"},
        {"СНЕГ", "СЕНО", "ВЕНОК", "ВИНО", "ВОСК", "СОВЕТ", "НОС", "СОН", "ВЕС", "СОК", "ГНОМ"},
        {"ПОРОГ", "ГОРОД", "ДОРОГА", "ПОРА", "РОГА", "ГОРА", "ДАР", "РОД", "ГОД", "ДОГ"},
        {"РУКА", "РЕЛЬС", "СЕЛО", "ЛЕС", "ЛУК", "РАК", "СУК", "КУРС", "КРУГ", "УКРА"}
    };
    // The curated word list is intentionally checked against each source word at runtime.
    static boolean possible(String word, String base) {
        String b=base.toUpperCase(Locale.ROOT), w=word.toUpperCase(Locale.ROOT);
        for (int i=0;i<w.length();i++) { int at=b.indexOf(w.charAt(i)); if(at<0)return false; b=b.substring(0,at)+b.substring(at+1); }
        return true;
    }
    class GameView extends View {
        final Paint p=new Paint(3); final SharedPreferences prefs; final ArrayList<Integer> selected=new ArrayList<>();
        final Set<String> found=new LinkedHashSet<>();
        int level,score,hints; String message="Составь слово из букв!"; float W,H,s,letterY,letterR; final ArrayList<RectF> letterBoxes=new ArrayList<>();
        RectF submit=new RectF(), clear=new RectF(), hint=new RectF(), next=new RectF();
        GameView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); prefs=getSharedPreferences("progress",MODE_PRIVATE); level=Math.max(0,Math.min(BASE.length-1,prefs.getInt("level",0))); score=prefs.getInt("score",0); hints=prefs.getInt("hints",3); loadFound(); }
        void loadFound(){found.clear();String raw=prefs.getString("found_"+level,"");if(!raw.isEmpty())found.addAll(Arrays.asList(raw.split(",")));}
        void save(){prefs.edit().putInt("level",level).putInt("score",score).putInt("hints",hints).putString("found_"+level,android.text.TextUtils.join(",",found)).apply();}
        int navy=Color.rgb(43,56,105), blue=Color.rgb(66,124,190), red=Color.rgb(222,99,91);
        void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
        void stroke(int color,float width){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);}
        void text(Canvas c,String t,float x,float y,float size,int color,boolean bold){fill(color);p.setTextSize(size);p.setTypeface(bold?android.graphics.Typeface.create("sans-serif",1):android.graphics.Typeface.create("sans-serif",0));p.setTextAlign(Paint.Align.CENTER);c.drawText(t,x,y,p);}
        void box(Canvas c,RectF r,float rad,int color){fill(color);c.drawRoundRect(r,rad,rad,p);}
        @Override protected void onDraw(Canvas original){super.onDraw(original);Canvas c=original; W=getWidth();H=getHeight();s=W/390f;float h=H/s;c.save();c.scale(s,s);
            c.drawColor(Color.rgb(250,251,247));stroke(Color.rgb(220,232,238),0.8f);for(int x=0;x<400;x+=22)c.drawLine(x,0,x,h,p);for(int y=0;y<h;y+=22)c.drawLine(0,y,390,y,p);
            text(c,"СЛОВА ИЗ СЛОВА",195,49,27,navy,true);
            text(c,"Уровень "+(level+1)+" / "+BASE.length,87,84,16,blue,true);text(c,"★ "+score,319,84,18,red,true);
            RectF banner=new RectF(20,102,370,182);box(c,banner,19,Color.rgb(237,244,253));stroke(blue,2.2f);c.drawRoundRect(banner,19,19,p);
            text(c,BASE[level],195,153,35,navy,true);
            text(c,"Найдено: "+found.size()+" / "+validWords().size(),195,214,17,navy,true);
            String current=current();RectF entry=new RectF(25,234,365,292);box(c,entry,14,Color.WHITE);stroke(Color.rgb(172,193,215),2);c.drawRoundRect(entry,14,14,p);
            text(c,current.isEmpty()?"Нажимай на буквы":current,195,273,current.isEmpty()?18:29,current.isEmpty()?blue:navy,!current.isEmpty());
            float rowY=350;letterBoxes.clear();String base=BASE[level];int n=base.length();float gap=6,unit=Math.min(49,(350f-(n-1)*gap)/n);float start=(390-(n*unit+(n-1)*gap))/2;
            for(int i=0;i<n;i++){float x=start+i*(unit+gap);RectF r=new RectF(x,rowY,x+unit,rowY+unit);letterBoxes.add(r);box(c,r,10,selected.contains(i)?Color.rgb(195,212,225):Color.WHITE);stroke(selected.contains(i)?red:blue,2.3f);c.drawRoundRect(r,10,10,p);text(c,base.substring(i,i+1),x+unit/2,rowY+unit*.72f,unit*.57f,navy,true);}
            submit=new RectF(25,420,245,474);clear=new RectF(255,420,365,474);hint=new RectF(25,493,365,545);next=new RectF(25,h-78,365,h-25);
            box(c,submit,14,blue);box(c,clear,14,Color.rgb(233,236,243));box(c,hint,14,Color.rgb(255,240,218));
            text(c,"ПРОВЕРИТЬ",135,455,18,Color.WHITE,true);text(c,"СБРОС",310,455,16,navy,true);text(c,"ПОДСКАЗКА ("+hints+")",195,527,17,navy,true);
            text(c,message,195,579,Math.min(16,330f/Math.max(1,message.length())*1.6f),red,true);
            float top=607;float available=Math.max(40,h-715);int maxRows=Math.max(1,(int)(available/27));int colCount=Math.max(1,(int)Math.ceil(found.size()/(double)maxRows));int i=0;
            for(String word:found){int col=i/maxRows, row=i%maxRows;float xx=(col+.5f)*390/colCount;text(c,"✓ "+word,xx,top+row*27,Math.min(17,320f/colCount/Math.max(4,word.length())*1.6f),navy,false);i++;}
            boolean done=found.containsAll(validWords());if(done){box(c,next,15,red);text(c,level==BASE.length-1?"ВСЕ УРОВНИ ПРОЙДЕНЫ!":"СЛЕДУЮЩИЙ УРОВЕНЬ →",195,h-43,17,Color.WHITE,true);}else{text(c,"Найди все слова, чтобы пройти дальше",195,h-43,13,blue,false);}
            c.restore();
        }
        Set<String> validWords(){Set<String> set=new LinkedHashSet<>();for(String w:WORDS[level])if(possible(w,BASE[level]))set.add(w);return set;}
        String current(){StringBuilder b=new StringBuilder();for(int i:selected)b.append(BASE[level].charAt(i));return b.toString();}
        void check(){String word=current();selected.clear();if(word.length()<3){message="Нужно минимум 3 буквы";return;}if(found.contains(word)){message="Это слово уже найдено";return;}if(validWords().contains(word)){found.add(word);score+=word.length()*10;message="Отлично! +"+(word.length()*10)+" очков";if(found.containsAll(validWords())){message="Уровень пройден! 🎉";hints++;}save();}else message="Такого слова нет в этом уровне";}
        void showHint(){if(hints<=0){message="Подсказки закончились";return;}for(String word:validWords())if(!found.contains(word)){hints--;found.add(word);message="Подсказка: "+word; if(found.containsAll(validWords())){message="Уровень пройден! 🎉";hints++;}save();return;}}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/s,y=e.getY()/s;for(int i=0;i<letterBoxes.size();i++)if(letterBoxes.get(i).contains(x,y)){if(selected.contains(i))selected.remove(Integer.valueOf(i));else selected.add(i);invalidate();return true;}
            if(submit.contains(x,y))check();else if(clear.contains(x,y)){selected.clear();message="Выбери буквы";}else if(hint.contains(x,y))showHint();else if(next.contains(x,y)&&found.containsAll(validWords())){if(level<BASE.length-1){level++;selected.clear();loadFound();message="Новый уровень!";save();}else message="Поздравляем! Ты прошёл игру!";}invalidate();return true;}
    }
}
