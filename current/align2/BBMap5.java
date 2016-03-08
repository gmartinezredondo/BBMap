package align2;

import java.io.File;
import java.util.ArrayList;

import stream.FASTQ;
import stream.FastaReadInputStream;
import stream.ReadStreamWriter;
import stream.SamLine;

import dna.ChromosomeArray;
import dna.Data;
import dna.Timer;
import fileIO.ReadWrite;

/**
 * Based on TestIndex11f
 * 
 * @author Brian Bushnell
 * @date Jan 3, 2013
 *
 */
public final class BBMap5 extends AbstractMapper  {
	

	public static void main(String[] args){
		Timer t=new Timer();
		t.start();
		BBMap5 mapper=new BBMap5(args);
		if(!INDEX_LOADED){mapper.loadIndex();}
		if(Data.scaffoldPrefixes){mapper.processAmbig2();}
		mapper.testSpeed(args);
		ReadWrite.waitForWritingToFinish();
		t.stop();
		sysout.println("\nTotal time:     \t"+t);
		clearStatics();
	}
	
	public BBMap5(String[] args){
		super(args);
	}
	
	@Override
	public void setDefaults(){
		ReadWrite.ZIPLEVEL=2;
		MAKE_MATCH_STRING=true;
		keylen=13;
		
		MINIMUM_ALIGNMENT_SCORE_RATIO=0.56f;

		keyDensity=1.9f;//2.3f;
		maxKeyDensity=3f;//4f;
		minKeyDensity=1.5f;//1.8f;
		maxDesiredKeys=15;
		
		SLOW_ALIGN_PADDING=4;
		SLOW_RESCUE_PADDING=4+SLOW_ALIGN_PADDING;
		TIP_SEARCH_DIST=100;
		
		MSA_TYPE="MultiStateAligner11ts";
		MAX_SITESCORES_TO_PRINT=5;
		PRINT_SECONDARY_ALIGNMENTS=false;
		AbstractIndex.MIN_APPROX_HITS_TO_KEEP=1;
	}
	
	@Override
	public String[] preparse(String[] args){
		if(fast){
			ArrayList<String> list=new ArrayList<String>();
			list.add("tipsearch="+TIP_SEARCH_DIST/5);
			list.add("maxindel=80");
			list.add("minhits=2");
			list.add("bwr=0.18");
			list.add("bw=40");
			list.add("minratio=0.65");
			list.add("midpad=150");
			list.add("minscaf=50");
			list.add("quickmatch=t");
//			list.add("k=13");
			
			BBIndex5.setFractionToExclude(BBIndex5.FRACTION_GENOME_TO_EXCLUDE*1.25f);
			
			for(String s : args){if(s!=null){list.add(s);}}
			args=list.toArray(new String[list.size()]);
			
			keyDensity*=0.9f;
			maxKeyDensity*=0.9f;
			minKeyDensity*=0.9f;
		}
		return args;
	}
	
	@Override
	void postparse(String[] args){
		
		if(MSA.bandwidthRatio>0 && MSA.bandwidthRatio<.2){
			SLOW_ALIGN_PADDING=Tools.min(SLOW_ALIGN_PADDING, 3);
			SLOW_RESCUE_PADDING=Tools.min(SLOW_RESCUE_PADDING, 6);
		}
		
		if(maxIndel1>-1){
			TIP_SEARCH_DIST=Tools.min(TIP_SEARCH_DIST, maxIndel1);
			BBIndex5.MAX_INDEL=maxIndel1;
		}
		if(maxIndel2>-1){
			BBIndex5.MAX_INDEL2=maxIndel2;
		}
		
		if(minApproxHits>-1){
			BBIndex5.MIN_APPROX_HITS_TO_KEEP=minApproxHits;
		}
		
		if(expectedSites>-1){
			BBMapThread5.setExpectedSites(expectedSites);
			sysout.println("Set EXPECTED_SITES to "+expectedSites);
		}
		
		if(fractionGenomeToExclude>=0){
			BBIndex5.setFractionToExclude(fractionGenomeToExclude);
		}
		
		{
			final String a=(args.length>0 ? args[0] : null);
			final String b=(args.length>1 ? args[1] : null);
			if(in1==null && a!=null && a.indexOf('=')<0 && (a.startsWith("stdin") || new File(a).exists())){in1=a;}
			if(in2==null && b!=null && b.indexOf('=')<0 && new File(b).exists()){in2=b;}
			if(ERROR_ON_NO_OUTPUT && !OUTPUT_READS && in1!=null){throw new RuntimeException("Error: no output file, and ERROR_ON_NO_OUTPUT="+ERROR_ON_NO_OUTPUT);}
		}

		assert(readlen<BBMapThread5.ALIGN_ROWS);
		
		if(MSA.bandwidth>0){
			int halfwidth=MSA.bandwidth/2;
			TIP_SEARCH_DIST=Tools.min(TIP_SEARCH_DIST, halfwidth/2);
			BBIndex5.MAX_INDEL=Tools.min(BBIndex5.MAX_INDEL, halfwidth/2);
			BBIndex5.MAX_INDEL2=Tools.min(BBIndex5.MAX_INDEL2, halfwidth);
			SLOW_ALIGN_PADDING=Tools.min(SLOW_ALIGN_PADDING, halfwidth/4);
			SLOW_RESCUE_PADDING=Tools.min(SLOW_RESCUE_PADDING, halfwidth/4);
		}
		
		if(PRINT_SECONDARY_ALIGNMENTS){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
		}
		
		if(ambigMode==AMBIG_BEST){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			if(!PRINT_SECONDARY_ALIGNMENTS){BBIndex5.QUIT_AFTER_TWO_PERFECTS=true;}
			sysout.println("Retaining first best site only for ambiguous mappings.");
		}else if(ambigMode==AMBIG_ALL){
			PRINT_SECONDARY_ALIGNMENTS=ReadStreamWriter.OUTPUT_SAM_SECONDARY_ALIGNMENTS=true;
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
			SamLine.MAKE_NH_TAG=true;
			ambiguousAll=true;
			sysout.println("Retaining all best sites for ambiguous mappings.");
		}else if(ambigMode==AMBIG_RANDOM){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
			ambiguousRandom=true;
			sysout.println("Choosing a site randomly for ambiguous mappings.");
		}else if(ambigMode==AMBIG_TOSS){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=true;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=true;
			sysout.println("Ambiguously mapped reads will be considered unmapped.");
		}else{
			throw new RuntimeException("Unknown ambiguous mapping mode: "+ambigMode);
		}
		
	}
	
	@Override
	public void setup(){
		
		assert(!useRandomReads || maxReads>0 || (in1!=null && in1.equals("sequential"))) : "Please specify number of reads to use.";
		
		if(minid!=-1){
			MINIMUM_ALIGNMENT_SCORE_RATIO=MSA.minIdToMinRatio(minid, MSA_TYPE);
			sysout.println("Set MINIMUM_ALIGNMENT_SCORE_RATIO to "+String.format("%.3f",MINIMUM_ALIGNMENT_SCORE_RATIO));
		}
		
		if(!setxs){SamLine.MAKE_XS_TAG=(SamLine.INTRON_LIMIT<1000000000);}
		if(setxs && !setintron){SamLine.INTRON_LIMIT=10;}
		
		if(outFile==null && outFile2==null && outFileM==null && outFileM2==null && outFileU==null && outFileU2==null && outFileB==null && outFileB2==null && BBSplitter.streamTable==null){
			sysout.println("No output file.");
			OUTPUT_READS=false;
		}else{
			OUTPUT_READS=true;
			if(bamscript!=null){
				BBSplitter.makeBamScript(bamscript, outFile, outFile2, outFileM, outFileM2, outFileU, outFileU2, outFileB, outFileB2);
			}
		}
		
		FastaReadInputStream.MIN_READ_LEN=Tools.max(keylen+2, FastaReadInputStream.MIN_READ_LEN);
		assert(FastaReadInputStream.settingsOK());
		
		if(build<0){throw new RuntimeException("Must specify a build number, e.g. build=1");}
		else{Data.GENOME_BUILD=build;}
		
		if(blacklist!=null && blacklist.size()>0){
			Timer t=new Timer();
			t.start();
			for(String s : blacklist){
				Blacklist.addToBlacklist(s);
			}
			t.stop();
			sysout.println("Created blacklist:\t"+t);
			t.start();
		}
		
		if(ziplevel!=-1){ReadWrite.ZIPLEVEL=ziplevel;}
		if(reference!=null){RefToIndex.makeIndex(reference, build, sysout, keylen);}
		ReadWrite.USE_GZIP=gzip;
		ReadWrite.USE_PIGZ=pigz;
	}
	

	@Override
	void processAmbig2(){
		assert(Data.scaffoldPrefixes) : "Only process this block if there are multiple references.";
		if(BBSplitter.AMBIGUOUS2_MODE==BBSplitter.AMBIGUOUS2_SPLIT){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
			sysout.println("Reads that map to multiple references will be written to special output streams.");
		}else if(BBSplitter.AMBIGUOUS2_MODE==BBSplitter.AMBIGUOUS2_FIRST){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
			sysout.println("Reads that map to multiple references will be written to the first reference's stream only.");
		}else if(BBSplitter.AMBIGUOUS2_MODE==BBSplitter.AMBIGUOUS2_TOSS){
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=true;
			sysout.println("Reads that map to multiple references will be considered unmapped.");
		}else if(BBSplitter.AMBIGUOUS2_MODE==BBSplitter.AMBIGUOUS2_RANDOM){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
			sysout.println("Reads that map to multiple references will be written to a random stream.");
		}else if(BBSplitter.AMBIGUOUS2_MODE==BBSplitter.AMBIGUOUS2_ALL){
			REMOVE_DUPLICATE_BEST_ALIGNMENTS=false;
			BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;
			sysout.println("Reads that map to multiple references will be written to all relevant output streams.");
		}else{
			BBSplitter.AMBIGUOUS2_MODE=BBSplitter.AMBIGUOUS2_FIRST;
		}
	}
	
	@Override
	void loadIndex(){
		Timer t=new Timer();
		t.start();
		
		if(build>-1){
			Data.setGenome(build);
			AbstractIndex.MINCHROM=1;
			AbstractIndex.MAXCHROM=Data.numChroms;
			if(minChrom<0){minChrom=1;}
			if(maxChrom<0 || maxChrom>Data.numChroms){maxChrom=Data.numChroms;}
			sysout.println("Set genome to "+Data.GENOME_BUILD);
			
			if(RefToIndex.AUTO_CHROMBITS){
				int maxLength=Tools.max(Data.chromLengths);
				RefToIndex.chrombits=Integer.numberOfLeadingZeros(maxLength); //Different for v5!
				RefToIndex.chrombits=Tools.min(RefToIndex.chrombits, 16);
			}
			if(RefToIndex.chrombits!=-1){
				BBIndex5.setChromBits(RefToIndex.chrombits);
				if(verbose_stats>0){sysout.println("Set CHROMBITS to "+RefToIndex.chrombits);}
			}
		}
		
		assert(minChrom>=AbstractIndex.MINCHROM && maxChrom<=AbstractIndex.MAXCHROM) :
			minChrom+", "+maxChrom+", "+AbstractIndex.MINCHROM+", "+AbstractIndex.MAXCHROM;
		AbstractIndex.MINCHROM=minChrom;
		AbstractIndex.MAXCHROM=maxChrom;
		
		if(targetGenomeSize>0){
			long bases=Data.numDefinedBases;
			long x=Tools.max(1, Math.round(0.25f+bases*1d/targetGenomeSize));
			BBMapThread5.setExpectedSites((int)x);
			sysout.println("Set EXPECTED_SITES to "+x);
		}
		
		assert(!(PERFECTMODE && SEMIPERFECTMODE));
		if(PERFECTMODE){setPerfectMode();}
		if(SEMIPERFECTMODE){setSemiperfectMode();}
		
		//Optional section for discrete timing of chrom array loading
		if(SLOW_ALIGN || AbstractIndex.USE_EXTENDED_SCORE || useRandomReads || MAKE_MATCH_STRING){
			sysout.println();
			if(RefToIndex.chromlist==null){
				Data.loadChromosomes(minChrom, maxChrom);
			}else{
				assert(RefToIndex.chromlist.size()==maxChrom-minChrom+1) : RefToIndex.chromlist.size();
				for(ChromosomeArray cha : RefToIndex.chromlist){
					Data.chromosomePlusMatrix[cha.chromosome]=cha;
				}
			}
			if(Shared.TRIM_READ_COMMENTS){Data.trimScaffoldNames();}
			t.stop();
			sysout.println("Loaded Reference:\t"+t);
			t.start();
		}
		RefToIndex.chromlist=null;
		
		t.start();
		BBIndex5.loadIndex(minChrom, maxChrom, keylen, !RefToIndex.NODISK, RefToIndex.NODISK);
		
		{
			long len=Data.numDefinedBases;
			if(len<300000000){
				BBIndex5.MAX_HITS_REDUCTION2+=1;
				BBIndex5.MAXIMUM_MAX_HITS_REDUCTION+=1;
				if(len<30000000){
					BBIndex5.setFractionToExclude(BBIndex5.FRACTION_GENOME_TO_EXCLUDE*0.5f);
					BBIndex5.MAXIMUM_MAX_HITS_REDUCTION+=1;
					BBIndex5.HIT_REDUCTION_DIV=Tools.max(BBIndex5.HIT_REDUCTION_DIV-1, 3);
				}else if(len<100000000){
					BBIndex5.setFractionToExclude(BBIndex5.FRACTION_GENOME_TO_EXCLUDE*0.6f);
				}else{
					BBIndex5.setFractionToExclude(BBIndex5.FRACTION_GENOME_TO_EXCLUDE*0.75f);
				}
			}
		}
		
		t.stop();
		sysout.println("Generated Index:\t"+t);
		t.start();
		
		if(!SLOW_ALIGN && !AbstractIndex.USE_EXTENDED_SCORE && !useRandomReads && !MAKE_MATCH_STRING){
			for(int chrom=minChrom; chrom<=maxChrom; chrom++){
				Data.unload(chrom, true);
			}
		}
		
		if(ReadWrite.countActiveThreads()>0){
			ReadWrite.waitForWritingToFinish();
			t.stop();
			sysout.println("Finished Writing:\t"+t);
			t.start();
		}
		
		if(!forceanalyze && (in1==null || maxReads==0)){return;}
		
		BBIndex5.analyzeIndex(minChrom, maxChrom, colorspace, BBIndex5.FRACTION_GENOME_TO_EXCLUDE, keylen);
		
		t.stop();
		sysout.println("Analyzed Index:   \t"+t);
		t.start();
	}
		
	public void testSpeed(String[] args){
		
		if(in1==null || maxReads==0){
			sysout.println("No reads to process; quitting.");
			return;
		}
		
		Timer t=new Timer();
		t.start();
		
		final boolean paired=openStreams(t, args);
		if(paired){BBIndex5.QUIT_AFTER_TWO_PERFECTS=false;}
		
		t.start();
		
		adjustThreadsforMemory(25);
		
		AbstractMapThread.CALC_STATISTICS=CALC_STATISTICS;
		AbstractMapThread[] mtts=new AbstractMapThread[Shared.THREADS];
		for(int i=0; i<mtts.length; i++){
			try {
				mtts[i]=new BBMapThread5(cris, keylen, 
						colorspace, SLOW_ALIGN, THRESH, minChrom, 
						maxChrom, keyDensity, maxKeyDensity, minKeyDensity, maxDesiredKeys, REMOVE_DUPLICATE_BEST_ALIGNMENTS, 
						SAVE_AMBIGUOUS_XY, MINIMUM_ALIGNMENT_SCORE_RATIO, TRIM_LIST, MAKE_MATCH_STRING, QUICK_MATCH_STRINGS, rosA, rosM, rosU, rosB, translateToBaseSpace,
						SLOW_ALIGN_PADDING, SLOW_RESCUE_PADDING, DONT_OUTPUT_UNMAPPED_READS, DONT_OUTPUT_BLACKLISTED_READS, MAX_SITESCORES_TO_PRINT, PRINT_SECONDARY_ALIGNMENTS,
						REQUIRE_CORRECT_STRANDS_PAIRS, SAME_STRAND_PAIRS, KILL_BAD_PAIRS, rcompMate, 
						PERFECTMODE, SEMIPERFECTMODE, FORBID_SELF_MAPPING, TIP_SEARCH_DIST,
						ambiguousRandom, ambiguousAll, KFILTER, TRIM_LEFT, TRIM_RIGHT, UNTRIM, TRIM_QUALITY, MIN_TRIM_LENGTH, LOCAL_ALIGN, RESCUE, STRICT_MAX_INDEL, MSA_TYPE);
			} catch (Exception e) {
				e.printStackTrace();
				abort(mtts, "Aborting due to prior error.");
			}  
			mtts[i].idmodulo=idmodulo;
			if(verbose){
				mtts[i].verbose=verbose;
				mtts[i].index().verbose=verbose;
			}
		}
		
		Thread cristhread=new Thread(cris);
		cristhread.start();
		sysout.println("Processing reads in "+(paired ? "paired" : "single")+"-ended mode.");
		sysout.println("Started read stream.");
		
		/* The threads are started after initialization to prevent resource competition between initialization and mapping */
		for(int i=0; i<mtts.length; i++){mtts[i].start();}
		sysout.println("Started "+mtts.length+" mapping thread"+(mtts.length==1 ? "" : "s")+".");
		
		final int broken=shutDownThreads(mtts, false);
		
		sysout.println("\n\n   ------------------   Results   ------------------   ");
		
		closeStreams(cris, rosA, rosM, rosU, rosB);
		sysout.println();
		printSettings(keylen);
		printOutput(mtts, t, keylen, paired, false);
		if(broken>0 || errorState){throw new RuntimeException("BBMap terminated in an error state; the output may be corrupt.");}
	}
	
	@Override
	void setSemiperfectMode() {
		assert(SEMIPERFECTMODE);
		if(SEMIPERFECTMODE){
			TRIM_LIST=false;
			keyDensity/=2;
			maxKeyDensity/=2;
			minKeyDensity=1.1f;
			maxDesiredKeys/=2;
			MINIMUM_ALIGNMENT_SCORE_RATIO=0.45f;
			BBIndex5.setSemiperfectMode();
		}
	}

	@Override
	void setPerfectMode() {
		assert(PERFECTMODE);
		if(PERFECTMODE){
			TRIM_LIST=false;
			keyDensity/=2;
			maxKeyDensity/=2;
			minKeyDensity=1.1f;
			maxDesiredKeys/=2;
			MINIMUM_ALIGNMENT_SCORE_RATIO=1.0f;
			BBIndex5.setPerfectMode();
		}
	}
	

	@Override
	void printSettings(int k){
		
		printSettings0(k, BBIndex5.MAX_INDEL, MINIMUM_ALIGNMENT_SCORE_RATIO);
		
		if(verbose_stats>=2){
			sysout.println("Key Density:          \t"+keyDensity+" ("+minKeyDensity+" ~ "+maxKeyDensity+")");
			sysout.println("Max keys:             \t"+maxDesiredKeys);
			
			sysout.println("Block Subsections:     \t"+BBIndex5.CHROMS_PER_BLOCK);
			sysout.println("Fraction To Remove:    \t"+String.format("%.4f", (BBIndex5.REMOVE_FREQUENT_GENOME_FRACTION ? BBIndex5.FRACTION_GENOME_TO_EXCLUDE : 0)));
			//		sysout.println("ADD_SCORE_Z:           \t"+Index5.ADD_SCORE_Z);
			sysout.println("Hits To Keep:          \t"+BBIndex5.MIN_APPROX_HITS_TO_KEEP);
		}
		
		if(verbose_stats>=3){
			sysout.println("Remove Clumpy:         \t"+BBIndex5.REMOVE_CLUMPY);
			if(BBIndex5.REMOVE_CLUMPY){
				sysout.println("CLUMPY_MAX_DIST:       \t"+BBIndex5.CLUMPY_MAX_DIST);
				sysout.println("CLUMPY_MIN_LENGTH:     \t"+BBIndex5.CLUMPY_MIN_LENGTH_INDEX);
				sysout.println("CLUMPY_FRACTION:       \t"+BBIndex5.CLUMPY_FRACTION);
			}
			sysout.println("Remove Long Lists:     \t"+BBIndex5.TRIM_LONG_HIT_LISTS);
			if(BBIndex5.TRIM_LONG_HIT_LISTS){
				sysout.println("HIT_FRACTION_TO_RETAIN:\t"+BBIndex5.HIT_FRACTION_TO_RETAIN);
			}
			sysout.println("Trim By Greedy:        \t"+BBIndex5.TRIM_BY_GREEDY);
			sysout.println("Trim By Total Sites:   \t"+BBIndex5.TRIM_BY_TOTAL_SITE_COUNT);
			if(BBIndex5.TRIM_BY_TOTAL_SITE_COUNT){
				sysout.println("MAX_AVG_SITES:         \t"+BBIndex5.MAX_AVERAGE_LIST_TO_SEARCH);
				sysout.println("MAX_AVG_SITES_2:       \t"+BBIndex5.MAX_AVERAGE_LIST_TO_SEARCH2);
				sysout.println("MAX_SHORTEST_SITE:     \t"+BBIndex5.MAX_SHORTEST_LIST_TO_SEARCH);
			}
			sysout.println("Index Min Score:       \t"+BBIndex5.MIN_SCORE_MULT);

			sysout.println("Dynamic Trim:          \t"+BBIndex5.DYNAMICALLY_TRIM_LOW_SCORES);
			if(BBIndex5.DYNAMICALLY_TRIM_LOW_SCORES){
				sysout.println("DYNAMIC_SCORE_THRESH:  \t"+BBIndex5.DYNAMIC_SCORE_THRESH);
			}
		}
		
	}

}
