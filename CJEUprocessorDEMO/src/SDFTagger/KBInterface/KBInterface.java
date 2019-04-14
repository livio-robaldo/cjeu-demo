package SDFTagger.KBInterface;

import java.util.ArrayList;
import SDFTagger.SDFItems.SDFHead;
public interface KBInterface 
{
    public void fillBagsOfSDFNode(String Form, String Lemma, ArrayList<String> bagsOnForm, ArrayList<String> bagsOnLemma)throws Exception;
    public ArrayList<String> retrieveSDFCodes(SDFHead SDFHead)throws Exception;
}
