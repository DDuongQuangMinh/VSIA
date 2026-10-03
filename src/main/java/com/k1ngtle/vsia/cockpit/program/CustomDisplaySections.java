package com.k1ngtle.vsia.cockpit.program;

import java.util.*;

public final class CustomDisplaySections {
    private CustomDisplaySections(){}
    public static int key(int code){if(code>=49&&code<=54)return code-48;if(code>=321&&code<=326)return code-320;return 0;}
    public static int section(DisplayDesign.Widget widget,int index){return widget.section()==0?Math.min(6,index+1):widget.section();}
    public static DisplayDesign select(DisplayDesign design,int section){List<DisplayDesign.Widget> widgets=new ArrayList<>();for(int i=0;i<design.widgets().size();i++)if(section(design.widgets().get(i),i)==section)widgets.add(design.widgets().get(i));return new DisplayDesign(design.theme(),widgets);}
    public record Bounds(int x,int y,int w,int h){}
    public static Bounds bounds(DisplayDesign design){if(design.widgets().isEmpty())return new Bounds(0,0,860,343);int x=860,y=343,right=0,bottom=0;for(var w:design.widgets()){x=Math.min(x,w.x());y=Math.min(y,w.y());right=Math.max(right,w.x()+w.w());bottom=Math.max(bottom,w.y()+w.h());}return new Bounds(x,y,right-x,bottom-y);}
    public static String title(DisplayDesign design,int section){var selected=select(design,section);if(selected.widgets().isEmpty())return "EMPTY";var first=selected.widgets().get(0);return first.text().isBlank()?first.type():first.text();}
}
