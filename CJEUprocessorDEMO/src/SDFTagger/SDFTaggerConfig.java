package SDFTagger;

import SDFTagger.KBInterface.KBInterface;
import java.io.File;

public abstract class SDFTaggerConfig 
{        
    protected KBInterface KBManager = null;
    protected SDFNodeConstraints SDFNodeConstraintsFactory = null;
    protected SDFLogger SDFLogger = null;
    protected File rootDirectoryBags = null;
    protected String[] localPathsBags = null;
    protected File rootDirectoryXmlSDFRules = null;
    protected File rootDirectoryCompiledSDFRules = null;
    protected String[] localPathsSDFRules = null;
    protected SDFTaggerConfig()throws Exception{SDFLogger=new SDFLogger(null);}
    protected SDFTaggerConfig(SDFTaggerConfig SDFTaggerConfig)
    {
        this.KBManager = SDFTaggerConfig.KBManager;
        this.SDFNodeConstraintsFactory = SDFTaggerConfig.SDFNodeConstraintsFactory;
        this.SDFLogger = SDFTaggerConfig.SDFLogger;
        this.rootDirectoryBags = SDFTaggerConfig.rootDirectoryBags;
        this.localPathsBags = SDFTaggerConfig.localPathsBags;
        this.rootDirectoryXmlSDFRules = SDFTaggerConfig.rootDirectoryXmlSDFRules;
        this.rootDirectoryCompiledSDFRules = SDFTaggerConfig.rootDirectoryCompiledSDFRules;
        this.localPathsSDFRules = SDFTaggerConfig.localPathsSDFRules;
    }
}
