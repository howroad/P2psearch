/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch;

import java.io.Serializable;

/**
 *
 * @author
 */
public class PageInfo implements Serializable,Comparable {
       public String docID;
       public String title;
       public String url;
       
       public PageInfo(String id,String title,String url){
           this.docID=id;
           this.title=title;
           this.url=url;
       }
       
       public boolean equals(PageInfo anInfo){
           if(anInfo.docID.equals(this.docID)){
               return true;
           }else{
               return false;
           }
       }
       

    @Override
    public int compareTo(Object t) {
        return this.docID.compareTo(((PageInfo)t).docID);
    }

    
}
