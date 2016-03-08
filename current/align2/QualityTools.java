package align2;

import java.util.Arrays;
import java.util.Random;

import stream.FASTQ;

import dna.AminoAcid;

import jgi.CalcTrueQuality;



/** 
 * 
 *  @author Brian Bushnell
 *  @date Jul 17, 2011 12:04:06 PM
 */
public class QualityTools {
	
	/*-------------------- Main --------------------*/

	public static void main(String[] args){
		
//		byte[] quals=new byte[] {15, 12, 20, 9, 10, 16, 14, 7, 11, 10, 10, 10, 10, 4, 4, 30, 30, 30, 30};
//		float[] probs=makeKeyProbs(quals, 4);
//		float[] probs2=makeKeyProbs(quals, 4);
//		
//		int[] scores=makeKeyScores(quals, 4, 50, 50, null);
//
//		System.out.println(Arrays.toString(probs)+"\n");
//		System.out.println(Arrays.toString(probs2)+"\n");
//		System.out.println(Arrays.toString(scores)+"\n");
//
//		bench(50, 20000000);
//		bench2(50, 20000000);
//		bench(50, 20000000);
//		bench2(50, 20000000);
//		bench(50, 20000000);
//		bench2(50, 20000000);
		
//		System.out.println(1-((1-.1f)*(1-.1f)*(1-.1f)));
//		System.out.println("\n"+Arrays.toString(PROB));
//		System.out.println("\n"+Arrays.toString(INVERSE));
//		System.out.println("\n"+Arrays.toString(SUB_PROB));
//		System.out.println("\n"+Arrays.toString(SUB_INVERSE));
		
//		initializeq102matrix(null);
//		for(int a=0; a<42; a++){
//			for(int b=0; b<42; b++){
//				for(int c=0; c<42; c++){
//					System.out.println(a+"\t"+b+"\t"+c+"\t"+q3ProbMatrix[a][b][c]);
//				}
//			}
//		}
		
	}
	
	/*-------------------- Constructors --------------------*/

	public QualityTools(){}
	
	/*-------------------- Methods --------------------*/

	/*-------------------- Overridden Methods --------------------*/

	/*-------------------- Abstract Methods --------------------*/

	/*-------------------- Static Methods --------------------*/
	
	public static void bench(int length, int rounds){
		
		long time=System.nanoTime();
		
		byte[] qual=new byte[length];
		for(int i=0; i<qual.length; i++){
			qual[i]=(byte)(Math.random()*30+5);
		}	
		for(int i=0; i<rounds; i++){
			float[] r=makeKeyProbs(qual, 8);
			if(r[r.length-1]>1 || r[r.length-1]<0){
				System.err.println("Ooops! "+Arrays.toString(r));
			}
		}
		
		time=System.nanoTime()-time;
		float seconds=(float)(time/1000000000d);
		System.out.println("Bench Time: "+String.format("%.3f",seconds)+" s");
	}
	
	public static void bench2(int length, int rounds){
		
		long time=System.nanoTime();
		
		byte[] qual=new byte[length];
		for(int i=0; i<qual.length; i++){
			qual[i]=(byte)(Math.random()*30+5);
		}	
		for(int i=0; i<rounds; i++){
			float[] r=makeKeyProbs2(qual, 8);
			if(r[r.length-1]>1 || r[r.length-1]<0){
				System.err.println("Ooops! "+Arrays.toString(r));
			}
		}
		
		time=System.nanoTime()-time;
		float seconds=(float)(time/1000000000d);
		System.out.println("Bench2 Time: "+String.format("%.3f",seconds)+" s");
	}
	
	public static int[] makeKeyScores(byte[] qual, int keylen, int range, int baseScore, int[] out){
		float[] probs=makeKeyProbs(qual, keylen);
		return makeKeyScores(probs, (qual.length-keylen+1), range, baseScore, out);
	}
	
	public static int[] makeKeyScores(float[] probs, int numProbs, int range, int baseScore, int[] out){
		if(out==null){out=new int[numProbs];}
//		assert(out.length==probs.length);
		assert(out.length>=numProbs);
		for(int i=0; i<numProbs; i++){
			out[i]=baseScore+(int)Math.round(range*(1-(probs[i])));
		}
		return out;
	}
	
	public static int[] makeIntScoreArray(byte[] qual, int maxScore, int[] out){
		if(out==null){out=new int[qual.length];}
		assert(out.length==qual.length);
		for(int i=0; i<qual.length; i++){
			float probM=PROB_CORRECT[qual[i]];
			out[i]=(int)Math.round(maxScore*probM);
		}
		return out;
	}
	
	public static byte[] makeByteScoreArray(byte[] qual, int maxScore, byte[] out, boolean negative){
		if(qual==null){return makeByteScoreArray(maxScore, out, negative);} 
		if(out==null){out=new byte[qual.length];}
		assert(out.length==qual.length);
		for(int i=0; i<qual.length; i++){
			float probM=PROB_CORRECT[qual[i]];
			int x=(int)Math.round(maxScore*probM);
			assert(x>=Byte.MIN_VALUE && x<=Byte.MAX_VALUE);
			if(negative){
				x=x-maxScore;
				assert(x<=0);
			}else{
				assert(x>=0 && x<=maxScore);
			}
			out[i]=(byte)x;
		}
		return out;
	}
	
	public static byte[] makeByteScoreArray(int maxScore, byte[] out, boolean negative){
		assert(out!=null);
//		for(int i=0; i<out.length; i++){
//			float probM=SUB_PROB[30];
//			int x=(int)Math.round(maxScore*probM);
//			assert(x>=Byte.MIN_VALUE && x<=Byte.MAX_VALUE);
//			if(negative){
//				x=x-maxScore;
//				assert(x<=0);
//			}else{
//				assert(x>=0 && x<=maxScore);
//			}
//			out[i]=(byte)x;
//		}
		Arrays.fill(out, (byte)0);
		return out;
	}
	
	/** Returns prob of error for each key */
	public static float[] makeKeyProbs(byte[] quality, int keylen){
		return makeKeyProbs(quality, keylen, null);
	}
	
	/** Returns prob of error for each key */
	public static float[] makeKeyProbs(byte[] quality, int keylen, float[] out){
		if(quality==null){return makeKeyProbs(keylen, out);}
		if(out==null){out=new float[quality.length-keylen+1];}
		assert(out.length>=quality.length-keylen+1) : quality.length+", "+keylen+", "+out.length;
//		assert(out.length==quality.length-keylen+1);
		float key1=1;
		
		int timeSinceZero=0;
		for(int i=0; i<keylen; i++){
//			byte q=(bases==null || bases[i]!='N' ? quality[i] : 0);
			byte q=quality[i];
			if(q>0){timeSinceZero++;}else{timeSinceZero=0;} //Tracks location of N's
			assert(q<PROB_CORRECT.length) : Arrays.toString(quality);
			float f=PROB_CORRECT[q];
			key1*=f;
		}
		out[0]=1-key1;
		if(timeSinceZero<keylen){out[0]=1;}
		
		for(int a=0, b=keylen; b<quality.length; a++, b++){
//			byte qa=(bases==null || bases[a]!='N' ? quality[a] : 0);
//			byte qb=(bases==null || bases[b]!='N' ? quality[b] : 0);
			byte qa=quality[a];
			byte qb=quality[b];
			if(qb>0){timeSinceZero++;}else{timeSinceZero=0;}
			float ipa=PROB_CORRECT_INVERSE[qa];
			float pb=PROB_CORRECT[qb];
			key1=key1*ipa*pb;
			out[a+1]=1-key1;
			if(timeSinceZero<keylen){out[a+1]=1;}
		}
		return out;
	}
	
	/** Returns prob of error for each key */
	public static float[] makeKeyProbs(int keylen, float[] out){
		assert(out!=null) : "Must provide array if no quality vector";
		Arrays.fill(out, 0);
		return out;
	}
	
	public static float[] makeKeyProbs2(byte[] quality, int keylen){
		float[] out=new float[quality.length-keylen+1];
		
		final int mid=out.length/2;
		
		float key1=1;
		float key2=1;
		for(int i=0, j=mid; i<keylen; i++, j++){
			byte q1=quality[i];
			float f1=PROB_CORRECT[q1];
			key1*=f1;
			byte q2=quality[j];
			float f2=PROB_CORRECT[q2];
			key2*=f2;
		}
		out[0]=1-key1;
		out[mid]=1-key2;
		
		for(int a=0, b=keylen, c=mid, d=mid+keylen; d<quality.length; 
				a++, b++, c++, d++){
			byte qa=quality[a];
			byte qb=quality[b];
			byte qc=quality[c];
			byte qd=quality[d];
			float ipa=PROB_CORRECT_INVERSE[qa];
			float ipc=PROB_CORRECT_INVERSE[qc];
			float pb=PROB_CORRECT[qb];
			float pd=PROB_CORRECT[qd];
			key1=key1*ipa*pb;
			key2=key2*ipc*pd;
			out[a+1]=1-key1;
			out[c+1]=1-key2;
		}
		return out;
	}

	public static byte[] makeQualityArray(int length, Random randyQual,
			int minQual, int maxQual, byte baseQuality, byte slant) {
		byte[] out=new byte[length];
		
		for(int i=0; i<length; i++){
			byte q=(byte)(baseQuality-(slant*i)/length);
			
			int hilo=randyQual.nextInt();
			
//			if((hilo&7)>0){
//				int range=Tools.max(1, maxQual-q+1);
//				int delta=Tools.min(randyQual.nextInt(range), randyQual.nextInt(range));
//				q=(byte)(q+delta);
//			}else{
//				int range=Tools.max(1, q-minQual+1);
//				int delta=Tools.min(randyQual.nextInt(range), randyQual.nextInt(range), randyQual.nextInt(range));
//				q=(byte)(q-delta);
//			}
			
			if((hilo&15)>0){
				int range=Tools.max(1, maxQual-q+1);
				int delta=(randyQual.nextInt(range)+randyQual.nextInt(range+1))/2;
				q=(byte)(q+delta);
			}else{
				int range=Tools.max(1, q-minQual+1);
				int delta=Tools.min(randyQual.nextInt(range), randyQual.nextInt(range));
				q=(byte)(q-delta);
			}
			q=(byte)Tools.min(Tools.max(q, minQual), maxQual);
			out[i]=q;
		}
		
		if(length>50){
			final int x=length/10;
			for(int i=0; i<x; i++){
				int y=x-i;
				out[i]=(byte)Tools.max(out[i]-(y+randyQual.nextInt(y+1))/2, minQual);
				out[length-i-1]=(byte)Tools.max(out[length-i-1]-(y+randyQual.nextInt(y+1))/2, minQual);
			}
		}
		
		return out;
	}
	
	public static int[] modifyOffsets(int[] offsets, float[] keyProbs) {
		if(offsets==null || offsets.length<3){return offsets;}

		int index=0;
		float max=keyProbs[offsets[0]];
		final int maxOffset=offsets[offsets.length-1];
		
		for(int i=1; i<offsets.length; i++){
			float f=keyProbs[offsets[i]];
			if(f>max){
				max=f;
				index=i;
			}
		}
		
		if(index==0 || index==offsets.length-1){return offsets;}
		if(max<.98f){return offsets;}
		
		final int removed=offsets[index];
		{
			int[] offsets2=new int[offsets.length-1];
			for(int i=0; i<index; i++){offsets2[i]=offsets[i];}
			for(int i=index; i<offsets2.length; i++){offsets2[i]=offsets[i+1];}
			offsets=offsets2;
			offsets2=null;
		}
		
		if(index==0){
			assert(false);
//			int i=offsets[0];
//			assert(i>removed && removed>=0);
//			while(i>removed && keyProbs[i-1]>=keyProbs[i]){i--;}
//			offsets[0]=i;
		}else if(index==offsets.length){
			assert(false);
//			int i=offsets[offsets.length-1];
//			assert(i<removed && removed==maxOffset);
//			while(i<removed && keyProbs[i+1]>=keyProbs[i]){i++;}
//			offsets[offsets.length-1]=i;
		}else if(offsets.length>2){
			if(index==offsets.length-1){
				assert(index>1);
				int i=offsets[index-1]; //5, 7, 9, 5, 6
				assert(i<removed && removed<maxOffset) : i+", "+removed+", "+maxOffset+", "+index+", "+offsets.length;
				while(i<removed-1 && keyProbs[i+1]>=keyProbs[i]){i++;}
				offsets[index-1]=i;
			}else{
				assert(index<offsets.length-1 && index>0);
				int i=offsets[index];
				assert(i>removed && removed>=0);
				while(i>removed+1 && keyProbs[i-1]>=keyProbs[i]){i--;}
				offsets[index]=i;
			}
		}
		
		return offsets;
	}
	

	/*-------------------- Fields --------------------*/

	/*-------------------- Final Fields --------------------*/

	/*-------------------- Static Fields --------------------*/
	
	/** Probability that this base is an error */
	public static final float[] PROB_ERROR=makeQualityToFloat(96);
	/** 1/PROB */
	public static final float[] PROB_ERROR_INVERSE=makeInverse(PROB_ERROR);
	
	public static final float[] PROB_CORRECT=oneMinus(PROB_ERROR);
	public static final float[] PROB_CORRECT_INVERSE=makeInverse(PROB_CORRECT);
	
	/*-------------------- Constants --------------------*/

	/*-------------------- Initializers --------------------*/
	
	public static final double phredToProbError(int phred){
		if(phred<1){return 1;}
		return Math.pow(10, 0-.1*phred);
	}
	
	public static byte probCorrectToPhred(double prob){
		double phred=probErrorToPhredDouble(1-prob);
//		System.out.println("phred1="+phred);
		phred=Math.round(phred);
//		System.out.println("phred2="+phred);
		byte q=(byte)Tools.mid(0, (int)phred, 41);
//		System.out.println("phred3="+q);
//		System.out.println("phred4="+(char)(q+33));
		return (byte)q;
	}
	
	public static double probErrorToPhredDouble(double prob){
		if(prob>=1){return 0;}
		if(prob<=0.000001){return 60;} 
		
		double phred=-10*Math.log10(prob);
		return phred;
	}
	
	private static final float[] makeQualityToFloat(int n){
		float[] r=new float[n];
		for(int i=0; i<n; i++){
			float x=(float)Math.pow(10, 0-.1*i);
			r[i]=x;
		}
		r[0]=.8f;
		return r;
	}
	
	private static final float[] makeInverse(float[] prob){
		float[] r=new float[prob.length];
		for(int i=0; i<r.length; i++){r[i]=1/prob[i];}
		return r;
	}
	
	private static final float[] oneMinus(float[] prob){
		float[] r=new float[prob.length];
		for(int i=0; i<r.length; i++){r[i]=1-prob[i];}
		return r;
	}

	/*-------------------- Notes --------------------*/

}
