package SDFTagger;

import java.util.*;
import org.jdom2.*;
import SDFTagger.SDFItems.*;

public class SDFRule 
{
    protected SDFNodeConstraints SDFNodeConstraintsFactory = null;
    protected long id = 0; 
    protected long priority = 0;
    protected SDFNodeConstraints SDFNodeConstraints = null;
    protected SDFRule owner = null;
    protected ArrayList<Prev> prevStarAlternatives = new ArrayList<Prev>();
    protected ArrayList<Prev> prevAlternatives = new ArrayList<Prev>();
    protected ArrayList<Next> nextStarAlternatives = new ArrayList<Next>();
    protected ArrayList<Next> nextAlternatives = new ArrayList<Next>();
    protected ArrayList<ArrayList<Dependent>> dependentsAlternatives = new ArrayList<ArrayList<Dependent>>();
    protected ArrayList<Governor> governorAlternatives = new ArrayList<Governor>();
    protected final int maxStarRangeInitValue=10000;
    protected int maxStarRange=10000;
    protected ArrayList<ArrayList<SDFTag>> stack = new ArrayList<ArrayList<SDFTag>>();
    protected void commit(){ArrayList<SDFTag> tags=stack.remove(0);for(int i=0;i<tags.size();i++)stack.get(0).add(tags.get(i));}
    protected void rollback(){stack.remove(0);}    
    protected ArrayList<Prev> prevsInPrevStarsCurrentlyInExecution = new ArrayList<Prev>();
    protected ArrayList<Next> nextsInNextStarsCurrentlyInExecution = new ArrayList<Next>();
    protected String saveSDFCode = null;
    protected SDFTagger SDFTagger = null;
    protected SDFLogger SDFLogger = null;
    public SDFRule(String SDFCode, SDFNodeConstraints SDFNodeConstraintsFactory, SDFTagger SDFTagger) throws Exception
    {   
        if(SDFCode==null)return;
        this.owner = this;
        this.SDFNodeConstraintsFactory = SDFNodeConstraintsFactory;
        this.SDFTagger = SDFTagger;
        this.SDFLogger = SDFTagger.SDFTaggerConfig.SDFLogger;
        id = Long.parseLong(SDFCode.substring(0, SDFCode.indexOf("£")));
        SDFCode = SDFCode.substring(SDFCode.indexOf("£")+1, SDFCode.length());
        priority = Long.parseLong(SDFCode.substring(0, SDFCode.indexOf("£")));
        SDFCode = SDFCode.substring(SDFCode.indexOf("£")+1, SDFCode.length());
        parseSDFCode(SDFCode);
    }

    public ArrayList<SDFTag> executeSDFRule(SDFNode node, long idInstance) throws Exception
    {
        stack.clear();
        if(checkConstraints(node, idInstance)==true)return stack.get(0);
        return new ArrayList<SDFTag>();
    }

    protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
    {
        ArrayList<SDFTag> tagsOnSDFNode = new ArrayList<SDFTag>();
        for(int i=0;i<SDFNodeConstraints.tags.size();i++)tagsOnSDFNode.add(new SDFTag(SDFNodeConstraints.tags.get(i), SDFNode.SDFHead, priority, id, idInstance));
        stack.add(0, tagsOnSDFNode);
        if(SDFNodeConstraints.doesSDFNodeMatch(SDFNode)==false)
        {
            String className = this.getClass().getName();
            if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
            if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
            if(className.compareToIgnoreCase("SDFRule")==0)return false;
            SDFLogger.startTracingOnSDFNode(this, idInstance, SDFNode);
            SDFLogger.endTracing(this, false);
            return false;
        }
        
        SDFLogger.startTracingOnSDFNode(this, idInstance, SDFNode);
        
        SDFNode prevSDFNode = SDFNode;
        if(prevStarAlternatives.size()>0)SDFLogger.stepInto(this, "prevStarAlternatives");
        for(int i=0;i<prevStarAlternatives.size();i++)
        {   
            Prev prev = prevStarAlternatives.get(i);
            prevsInPrevStarsCurrentlyInExecution.add(prev);
            boolean ok = prev.checkConstraints(prevSDFNode, idInstance);
            prevsInPrevStarsCurrentlyInExecution.remove(prev);
            if(ok==true)
            {
                prevSDFNode=prev.leftmostSDFNode;
                i=-1;
            }
            
            maxStarRange--;
            if(maxStarRange==0)break;
        }
        maxStarRange=maxStarRangeInitValue;
        if(prevStarAlternatives.size()>0)SDFLogger.stepOut();
        
        boolean ok = false;
        if(prevAlternatives.size()>0)SDFLogger.stepInto(this, "prevAlternatives");
        for(int i=0;(i<prevAlternatives.size())&&(ok==false);i++)ok=prevAlternatives.get(i).checkConstraints(prevSDFNode, idInstance);
        if(prevAlternatives.size()>0)SDFLogger.stepOut();
        if((ok == false)&&(prevAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }
        
        SDFNode nextSDFNode = SDFNode;
        if(nextStarAlternatives.size()>0)SDFLogger.stepInto(this, "nextStarAlternatives");
        for(int i=0;i<nextStarAlternatives.size();i++)
        {
            Next next = nextStarAlternatives.get(i);
            nextsInNextStarsCurrentlyInExecution.add(next);
            ok = next.checkConstraints(nextSDFNode, idInstance);
            nextsInNextStarsCurrentlyInExecution.remove(next);
            if(ok==true)
            {
                nextSDFNode=next.rightmostSDFNode;
                i=-1;
            }
            
            maxStarRange--;
            if(maxStarRange==0)break;
        }
        maxStarRange=maxStarRangeInitValue;
        if(nextStarAlternatives.size()>0)SDFLogger.stepOut();
        
        ok = false;
        if(nextAlternatives.size()>0)
            SDFLogger.stepInto(this, "nextAlternatives");
        for(int i=0;(i<nextAlternatives.size())&&(ok==false);i++)
            ok=nextAlternatives.get(i).checkConstraints(nextSDFNode, idInstance);
        if(nextAlternatives.size()>0)
            SDFLogger.stepOut();
        if((ok==false)&&(nextAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }
        
        ArrayList<Prev> tempSavePrev = new ArrayList<Prev>(); 
        for(int i=0;i<prevsInPrevStarsCurrentlyInExecution.size();i++)tempSavePrev.add(prevsInPrevStarsCurrentlyInExecution.get(i));
        prevsInPrevStarsCurrentlyInExecution.clear();
        ArrayList<Next> tempSaveNext = new ArrayList<Next>(); 
        for(int i=0;i<nextsInNextStarsCurrentlyInExecution.size();i++)tempSaveNext.add(nextsInNextStarsCurrentlyInExecution.get(i));
        nextsInNextStarsCurrentlyInExecution.clear();

        ok = false;
        if(governorAlternatives.size()>0)SDFLogger.stepInto(this, "governorAlternatives");
        for(int i=0;(i<governorAlternatives.size())&&(ok==false);i++)ok=governorAlternatives.get(i).checkConstraints(SDFNode, idInstance);
        if(governorAlternatives.size()>0)SDFLogger.stepOut();
        if((ok==false)&&(governorAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }
        
        ok = false;
        if(dependentsAlternatives.size()>0)SDFLogger.stepInto(this, "dependentsAlternatives");
        for(int i=0; (i<dependentsAlternatives.size())&&(ok==false); i++)
        {
            ArrayList<Dependent> dependents = new ArrayList<Dependent>();
            for(int j=0; j<dependentsAlternatives.get(i).size(); j++)dependents.add(dependentsAlternatives.get(i).get(j));
            SDFLogger.stepInto(this, "dependents");
            ok = matchTuplesOfDependents(dependents, SDFNode, id, idInstance, priority);
            SDFLogger.stepOut();
        }
        if(dependentsAlternatives.size()>0)SDFLogger.stepOut();
        if((ok==false)&&(dependentsAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }
        
        for(int i=0;i<tempSavePrev.size();i++)prevsInPrevStarsCurrentlyInExecution.add(tempSavePrev.get(i));
        for(int i=0;i<tempSaveNext.size();i++)nextsInNextStarsCurrentlyInExecution.add(tempSaveNext.get(i));
        
        for(int i=0;i<prevsInPrevStarsCurrentlyInExecution.size();i++)
            if((prevsInPrevStarsCurrentlyInExecution.get(i).leftmostSDFNode==null)||(SDFNode.index<prevsInPrevStarsCurrentlyInExecution.get(i).leftmostSDFNode.index))
                prevsInPrevStarsCurrentlyInExecution.get(i).leftmostSDFNode=SDFNode;
        for(int i=0;i<nextsInNextStarsCurrentlyInExecution.size();i++)
            if((nextsInNextStarsCurrentlyInExecution.get(i).rightmostSDFNode==null)||(SDFNode.index>nextsInNextStarsCurrentlyInExecution.get(i).rightmostSDFNode.index))
                nextsInNextStarsCurrentlyInExecution.get(i).rightmostSDFNode=SDFNode;
        
        SDFLogger.endTracing(this, true);
        return true;
    }
    
    protected class Prev extends SDFRule
    {
        protected int maxDistance = -1;
        protected boolean not = false;
        protected SDFNode leftmostSDFNode = null;
        
        public Prev(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            saveSDFCode = SDFCode;
        }
        
        protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
        {
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            if((SDFNode.prevSDFNode==null)||(SDFNode.prevSDFNode.SDFHead==null))
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                SDFLogger.endTracing(this, not);
                return not;
            }
            
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxDistance(SDFNode, maxDistance);            
            SDFNode prevSatisfyingSDFNode=null;
            for(int i=0; (i<SDFNodes.size())&&(prevSatisfyingSDFNode==null); i++)
            {
                if(super.checkConstraints(SDFNodes.get(i), idInstance)==true)prevSatisfyingSDFNode=SDFNodes.get(i);
                else rollback();
            }
            if(prevSatisfyingSDFNode==null)
            {
                SDFLogger.endTracing(this, not);
                return not;
            }
            else
            {
                if(not==true)rollback();
                else commit();
                SDFLogger.endTracing(this, !not);
                return !not;
            }
        }

        private ArrayList<SDFNode> getSDFNodesAtMaxDistance(SDFNode SDFNode, int distance)
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
            SDFNode tempSDFNode = SDFNode.prevSDFNode;
            while((tempSDFNode!=null)&&(distance>0)&&(tempSDFNode.SDFHead!=null))
            {
                ret.add(tempSDFNode);
                tempSDFNode = tempSDFNode.prevSDFNode;
                distance--;
            }
            return ret;
        }
        
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)
            {
                not=true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            if(SDFCode.indexOf("£C")==0)
            {
                maxDistance = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
                super.parseSDFCode(SDFCode);
            }
            if(maxDistance==-1)throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxDistance");
        }
    }

    protected class Next extends SDFRule
    {
        protected int maxDistance = -1;
        protected boolean not = false;        
        protected SDFNode rightmostSDFNode = null;
        public Next(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            saveSDFCode = SDFCode;
        }
        
        protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
        {
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            if((SDFNode.nextSDFNode==null)||(SDFNode.nextSDFNode.SDFHead==null))
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                SDFLogger.endTracing(this, not);
                return not;
            }
            
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxDistance(SDFNode, maxDistance);
            SDFNode nextSatisfyingSDFNode=null;
            for(int i=0; (i<SDFNodes.size())&&(nextSatisfyingSDFNode==null); i++)
            {
                if(super.checkConstraints(SDFNodes.get(i), idInstance)==true)nextSatisfyingSDFNode=SDFNodes.get(i);
                else rollback();
            }
            if(nextSatisfyingSDFNode==null)
            {
                SDFLogger.endTracing(this, not);
                return not;
            }
            else
            {
                if(not==true)rollback();
                else commit();
                SDFLogger.endTracing(this, !not);
                return !not;
            }
        }
        
        private ArrayList<SDFNode> getSDFNodesAtMaxDistance(SDFNode SDFNode, int distance)
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();    
            SDFNode tempSDFNode = SDFNode.nextSDFNode;
            while((tempSDFNode!=null)&&(distance>0))
            {
                ret.add(tempSDFNode);
                tempSDFNode = tempSDFNode.nextSDFNode;
                distance--;
            }
                
            return ret;
        }
        
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)
            {
                not = true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            if(SDFCode.indexOf("£C")==0)
            {
                maxDistance = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
                super.parseSDFCode(SDFCode);
            }
            
            if(maxDistance==-1)throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxDistance");
        }
    }
    
    protected class Governor extends SDFRule
    {
        protected int maxHeight = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();
        public Governor(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            saveSDFCode = SDFCode;
        }
        
        protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
        {
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            if(SDFNode.SDFHead.getGovernor()==null)
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                SDFLogger.endTracing(this, not);
                return not;
            }
            
            Hashtable<SDFNode,SDFNode> SDFNodes2FirstDependent = new Hashtable<SDFNode,SDFNode>();
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxHeight(SDFNode, maxHeight, SDFNodes2FirstDependent);
            
            SDFNode upSatisfyingSDFNode=null;
            for(int i=0; (i<SDFNodes.size())&&(upSatisfyingSDFNode==null); i++)
            {
                if(labelAlternatives.isEmpty()==false)
                {
                    boolean ok = false;
                    for(int j=0; (j<labelAlternatives.size())&&(ok==false); j++)
                        if(SDFNodes2FirstDependent.get(SDFNodes.get(i)).SDFHead.getLabel().compareToIgnoreCase(labelAlternatives.get(j))==0)
                            ok=true;
                    if(ok==false)continue;
                }
                if(super.checkConstraints(SDFNodes.get(i), idInstance)==true)upSatisfyingSDFNode=SDFNodes.get(i);
                else rollback();
            }
            
            if(upSatisfyingSDFNode==null)
            {
                SDFLogger.endTracing(this, not);
                return not;
            }
            else
            {
                if(not==true)rollback();
                else commit();
                SDFLogger.endTracing(this, !not);
                return !not;
            }
        }
        
        protected ArrayList<SDFNode> getSDFNodesAtMaxHeight(SDFNode node, int height, Hashtable<SDFNode,SDFNode> SDFNodes2FirstDependent) throws Exception
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
            if((node==null)||(height<=0)) return new ArrayList<SDFNode>();
            
            SDFHead governor = node.SDFHead.getGovernor();
            if(governor==null)return ret;
            SDFNode SDFNodeOfGovernor = owner.SDFTagger.SDFHead2SDFNode.get(governor);
            ret.add(SDFNodeOfGovernor);
            SDFNodes2FirstDependent.put(SDFNodeOfGovernor, node);
            ArrayList<SDFNode> subNodes = getSDFNodesAtMaxHeight(SDFNodeOfGovernor, height-1, SDFNodes2FirstDependent);
            for(int j=0; j<subNodes.size(); j++) ret.add(subNodes.get(j));
            
            return ret;
        }
        
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)
            {
                not = true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            while(SDFCode.indexOf("£E")==0)
            {
                labelAlternatives.add(SDFCode.substring(2, SDFCode.indexOf("£E",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£E",2)+2, SDFCode.length());
            }
         
            if(SDFCode.indexOf("£C")==0)
            {
                maxHeight = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
            }
            
            if(maxHeight==-1)
                throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxHeight");

            super.parseSDFCode(SDFCode);
        }
    }
    
    protected class Dependent extends SDFRule
    {
        protected int maxDepth = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();
        
        public Dependent(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            saveSDFCode = SDFCode;
        }
        
        protected void checkConstraints(SDFNode SDFNodeGovernor, long idInstance, Hashtable<SDFNode, ArrayList<SDFTag>> htDependentSDNodes2Tags)throws Exception
        {   
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxDepth(SDFNodeGovernor, maxDepth);
            if(SDFNodes.isEmpty())
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                SDFLogger.endTracing(this, not);
                return;
            }

            for(int i=0; i<SDFNodes.size(); i++)
            {
                if(labelAlternatives.isEmpty()==false)
                {
                    boolean ok = false;
                    for(int j=0; (j<labelAlternatives.size())&&(ok==false); j++)
                        if(SDFNodes.get(i).SDFHead.getLabel().compareToIgnoreCase(labelAlternatives.get(j))==0)
                            ok=true;
                    if(ok==false)continue;
                }
                
                boolean ok = super.checkConstraints(SDFNodes.get(i), idInstance);
                if(ok==true)htDependentSDNodes2Tags.put(SDFNodes.get(i), stack.get(0));
                SDFLogger.endTracing(this, ok^not);
                rollback();
            }
        }
        
        protected ArrayList<SDFNode> getSDFNodesAtMaxDepth(SDFNode node, int depth) throws Exception
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
            if((node==null)||(depth<=0)) return new ArrayList<SDFNode>();
            SDFHead[] depsSDFHead = node.SDFHead.getDependents();
            if((depsSDFHead==null)||(depsSDFHead.length==0)) return ret;
            for(int i=0; i<depsSDFHead.length; i++)
            {
                SDFNode SDFNodeOfDependent = owner.SDFTagger.SDFHead2SDFNode.get(depsSDFHead[i]);
                ret.add(SDFNodeOfDependent);
                ArrayList<SDFNode> subNodes = getSDFNodesAtMaxDepth(SDFNodeOfDependent, depth-1);
                for(int j=0; j<subNodes.size(); j++) ret.add(subNodes.get(j));
            }
            return ret;
        }
        
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)
            {
                not = true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            while(SDFCode.indexOf("£E")==0)
            {
                labelAlternatives.add(SDFCode.substring(2, SDFCode.indexOf("£E",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£E",2)+2, SDFCode.length());
            }
            
            if(SDFCode.indexOf("£C")==0)
            {
                maxDepth = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
            }
                        
            if(maxDepth==-1) throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxDepth in <Dependent>");

            super.parseSDFCode(SDFCode);
        }
    }
    
    protected boolean matchTuplesOfDependents(ArrayList<Dependent> dependents, SDFNode SDFNodeGovernor, long id, long idInstance, long priority) throws Exception
    {
        Hashtable<Dependent, ArrayList<SDFNode>> possibleAssignments = new Hashtable<Dependent, ArrayList<SDFNode>>();
        Hashtable<Dependent, Hashtable<SDFNode, ArrayList<SDFTag>>> dependents2SDFTags = new Hashtable<Dependent, Hashtable<SDFNode, ArrayList<SDFTag>>>();
        SDFNode[] assignments = new SDFNode[dependents.size()];
        Hashtable<Dependent, ArrayList<SDFNode>> otherOptions = new Hashtable<Dependent, ArrayList<SDFNode>>();
        
        for(int i=0;i<dependents.size();i++)
        {
            Hashtable<SDFNode, ArrayList<SDFTag>> htDependentSDNodes2Tags = new Hashtable<SDFNode, ArrayList<SDFTag>>();
            dependents2SDFTags.put(dependents.get(i), htDependentSDNodes2Tags);   
            dependents.get(i).checkConstraints(SDFNodeGovernor, idInstance, htDependentSDNodes2Tags);
            ArrayList<SDFNode> tempSDFNodes = new ArrayList<SDFNode>();
            Enumeration en = htDependentSDNodes2Tags.keys();
            while(en.hasMoreElements())tempSDFNodes.add((SDFNode)en.nextElement());
            possibleAssignments.put(dependents.get(i), tempSDFNodes);
            otherOptions.put(dependents.get(i), new ArrayList<SDFNode>());
            assignments[i]=null;
        }
        
        int index = 0;
        while((index>-1)&&(index<dependents.size()))
        {
            ArrayList<SDFNode> validPossibleAssignments = possibleAssignments.get(dependents.get(index));
            ArrayList<SDFNode> invalidPossibleAssignments = otherOptions.get(dependents.get(index));
            boolean assignmentFound=false;
            if((dependents.get(index).not==true)&&(validPossibleAssignments.isEmpty()==false)){assignmentFound=false;}
            else if((dependents.get(index).not==true)&&(validPossibleAssignments.isEmpty()==true)){assignmentFound=true;}
            else if((dependents.get(index).not==false)&&(validPossibleAssignments.isEmpty()==true)){assignmentFound=false;}
            else if((dependents.get(index).not==false)&&(validPossibleAssignments.isEmpty()==false))
            {
                assignments[index] = validPossibleAssignments.remove(0);
                invalidPossibleAssignments.add(assignments[index]);
                for(int i=index+1;i<dependents.size();i++)
                    if(possibleAssignments.get(dependents.get(i)).remove(assignments[index])==true)
                        otherOptions.get(dependents.get(i)).add(assignments[index]);
                assignmentFound=true;
            }
            
            if(assignmentFound==true)index++;
            else
            {
                while(invalidPossibleAssignments.isEmpty()==false)validPossibleAssignments.add(invalidPossibleAssignments.remove(0));
                index--;
            }
        }
        
        if(index==-1)return false;
        for(int i=0;i<assignments.length;i++)
        {
            if(assignments[i]==null)continue;    
            ArrayList<SDFTag> SDFTags = dependents2SDFTags.get(dependents.get(i)).get(assignments[i]);
            for(int j=0;j<SDFTags.size();j++)stack.get(0).add(SDFTags.get(j));
        }
        
        return true;
    }
    
    protected void parseSDFCode(String SDFCode) throws Exception
    {
        String SDFCodeOfSDFNode = SDFCode;
        int index = SDFCode.indexOf("ç");
        int otherIndex = SDFCode.indexOf("%");
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf("?");
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf("!");
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf(";");
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf("&");
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        if(index!=-1)
        {
            SDFCodeOfSDFNode = SDFCodeOfSDFNode.substring(0, index);
            SDFCode = SDFCode.substring(index, SDFCode.length());
            if(SDFCodeOfSDFNode.indexOf("$")==SDFCodeOfSDFNode.lastIndexOf("$"))
            {
                SDFCodeOfSDFNode = SDFCodeOfSDFNode+SDFCode.substring(0, SDFCode.indexOf("$")+1);
                SDFCode = SDFCode.substring(SDFCode.indexOf("$")+1, SDFCode.length());
            }
        }
        
        SDFNodeConstraints = SDFNodeConstraintsFactory.FactorySDFNodeConstraints();
        SDFNodeConstraints.loadAttributes(SDFCodeOfSDFNode, this);
        if(SDFCode.indexOf("ç")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String prevStarAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(prevStarAlternativesCode.isEmpty()==false)
            {
                counter =  prevStarAlternativesCode.substring(0, prevStarAlternativesCode.indexOf(",")+1);
                String prevCode = prevStarAlternativesCode.substring(counter.length(), prevStarAlternativesCode.lastIndexOf(counter));
                prevStarAlternativesCode = prevStarAlternativesCode.substring(prevStarAlternativesCode.lastIndexOf(counter)+counter.length(), prevStarAlternativesCode.length());
                prevStarAlternatives.add(new Prev(prevCode, owner));
            }
        }
        
        if(SDFCode.indexOf("%")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String prevAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(prevAlternativesCode.isEmpty()==false)
            {
                counter =  prevAlternativesCode.substring(0, prevAlternativesCode.indexOf(",")+1);
                String prevCode = prevAlternativesCode.substring(counter.length(), prevAlternativesCode.lastIndexOf(counter));
                prevAlternativesCode = prevAlternativesCode.substring(prevAlternativesCode.lastIndexOf(counter)+counter.length(), prevAlternativesCode.length());
                prevAlternatives.add(new Prev(prevCode, owner));
            }
        }
        
        if(SDFCode.indexOf("?")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String nextStarAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(nextStarAlternativesCode.isEmpty()==false)
            {
                counter =  nextStarAlternativesCode.substring(0, nextStarAlternativesCode.indexOf(",")+1);
                String nextCode = nextStarAlternativesCode.substring(counter.length(), nextStarAlternativesCode.lastIndexOf(counter));
                nextStarAlternativesCode = nextStarAlternativesCode.substring(nextStarAlternativesCode.lastIndexOf(counter)+counter.length(), nextStarAlternativesCode.length());
                nextStarAlternatives.add(new Next(nextCode, owner));
            }
        }
        
        if(SDFCode.indexOf("!")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String nextAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(nextAlternativesCode.isEmpty()==false)
            {
                counter =  nextAlternativesCode.substring(0, nextAlternativesCode.indexOf(",")+1);
                String nextCode = nextAlternativesCode.substring(counter.length(), nextAlternativesCode.lastIndexOf(counter));
                nextAlternativesCode = nextAlternativesCode.substring(nextAlternativesCode.lastIndexOf(counter)+counter.length(), nextAlternativesCode.length());
                nextAlternatives.add(new Next(nextCode, owner));
            }
        }
        
        if(SDFCode.indexOf("&")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String governorAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(governorAlternativesCode.isEmpty()==false)
            {
                counter =  governorAlternativesCode.substring(0, governorAlternativesCode.indexOf(",")+1);
                String governorCode = governorAlternativesCode.substring(counter.length(), governorAlternativesCode.lastIndexOf(counter));
                governorAlternativesCode = governorAlternativesCode.substring(governorAlternativesCode.lastIndexOf(counter)+counter.length(), governorAlternativesCode.length());
                governorAlternatives.add(new Governor(governorCode, owner));
            }
        }
        
        if(SDFCode.indexOf(";")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String dependentsAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(dependentsAlternativesCode.isEmpty()==false)
            {
                ArrayList<Dependent> dependents = new ArrayList<Dependent>();
                dependentsAlternatives.add(dependents);
                
                counter =  dependentsAlternativesCode.substring(0, dependentsAlternativesCode.indexOf(",")+1);
                String dependentsCode = dependentsAlternativesCode.substring(counter.length(), dependentsAlternativesCode.lastIndexOf(counter));
                dependentsAlternativesCode = dependentsAlternativesCode.substring(dependentsAlternativesCode.lastIndexOf(counter)+counter.length(), dependentsAlternativesCode.length());
                
                while(dependentsCode.isEmpty()==false)
                {
                    counter =  dependentsCode.substring(0, dependentsCode.indexOf(",")+1);
                    String dependentCode = dependentsCode.substring(counter.length(), dependentsCode.lastIndexOf(counter));
                    dependentsCode = dependentsCode.substring(dependentsCode.lastIndexOf(counter)+counter.length(), dependentsCode.length());
                    dependents.add(new Dependent(dependentCode, owner));
                }
            }
        }
    }
    
    public Element buildSDFRuleXML()
    {
        try
        {
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
        }catch(Exception e){return new Element("Error in generating the <SDFDebug> XML Element");}
            
        String className = this.getClass().getName();
        if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
        if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
        Element SDFRuleStep = new Element(className.toLowerCase());
        
        if(className.compareToIgnoreCase("sdfrule")==0)
        {
            SDFRuleStep = new Element("SDFRule");
            SDFRuleStep.setAttribute("id", ""+id);
            SDFRuleStep.setAttribute("priority", ""+priority);
        }
        else
        {
            if((this instanceof Prev)&&((Prev)this).not==true)SDFRuleStep=new Element("notPrev");
            else if((this instanceof Next)&&((Next)this).not==true)SDFRuleStep=new Element("notNext");
            else if((this instanceof Governor)&&((Governor)this).not==true)SDFRuleStep=new Element("notGovernor");
            else if((this instanceof Dependent)&&((Dependent)this).not==true)SDFRuleStep=new Element("notDependent");
            if(this instanceof Prev)SDFRuleStep.setAttribute("maxDistance", ""+((Prev)this).maxDistance);
            else if(this instanceof Next)SDFRuleStep.setAttribute("maxDistance", ""+((Next)this).maxDistance);
            else if(this instanceof Governor)SDFRuleStep.setAttribute("maxHeight", ""+((Governor)this).maxHeight);
            else if(this instanceof Dependent)SDFRuleStep.setAttribute("maxDepth", ""+((Dependent)this).maxDepth);
        }
        
        Element[] tagsAndHeadAlternatives = SDFNodeConstraints.buildTagsAndHeadAlternativesXML();
        for(int i=0;i<tagsAndHeadAlternatives.length;i++)SDFRuleStep.getContent().add(tagsAndHeadAlternatives[i]);
        
        if(prevStarAlternatives.size()>0)
        {
            Element prevStarAlternatives = new Element("prevStarAlternatives");
            SDFRuleStep.getContent().add(prevStarAlternatives);
            for(int i=0;i<this.prevStarAlternatives.size();i++)prevStarAlternatives.getContent().add(this.prevStarAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(prevAlternatives.size()>0)
        {
            Element prevAlternatives = new Element("prevAlternatives");
            SDFRuleStep.getContent().add(prevAlternatives);
            for(int i=0;i<this.prevAlternatives.size();i++)prevAlternatives.getContent().add(this.prevAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(nextStarAlternatives.size()>0)
        {
            Element nextStarAlternatives = new Element("nextStarAlternatives");
            SDFRuleStep.getContent().add(nextStarAlternatives);
            for(int i=0;i<this.nextStarAlternatives.size();i++)nextStarAlternatives.getContent().add(this.nextStarAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(nextAlternatives.size()>0)
        {
            Element nextAlternatives = new Element("nextAlternatives");
            SDFRuleStep.getContent().add(nextAlternatives);
            for(int i=0;i<this.nextAlternatives.size();i++)nextAlternatives.getContent().add(this.nextAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(governorAlternatives.size()>0)
        {
            Element governorAlternatives = new Element("governorAlternatives");
            SDFRuleStep.getContent().add(governorAlternatives);
            for(int i=0;i<this.governorAlternatives.size();i++)governorAlternatives.getContent().add(this.governorAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(dependentsAlternatives.size()>0)
        {
            Element dependentsAlternatives = new Element("dependentsAlternatives");
            SDFRuleStep.getContent().add(dependentsAlternatives);
            
            for(int j=0;j<this.dependentsAlternatives.size();j++)
            {
                Element dependents = new Element("dependents");
                dependentsAlternatives.getContent().add(dependents);
                for(int i=0;i<this.dependentsAlternatives.get(j).size();i++)dependents.getContent().add(this.dependentsAlternatives.get(j).get(i).buildSDFRuleXML());
            }
        }
        
        return SDFRuleStep;
    }
}
