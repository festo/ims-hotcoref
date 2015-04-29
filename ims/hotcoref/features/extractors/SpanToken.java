package ims.hotcoref.features.extractors;

public enum SpanToken {
	Hd, HdGov, HdLmc, HdRmc, HdLs, HdRs,
	//Previous (P) and prev-prev (PP), and next (N) and next-next (NN), regardless of within span boundary
	HdP,
	HdPP,
	HdN,
	HdNN,
	//Previous (IP) and prev-prev (IPP), and next (IN) and next-next (INN), but only inside span boundary	
	HdIP,
	HdIPP,
	HdIN,
	HdINN,
	//Span first and last
	SF,
	SL,
	//Span preceeding and following
	SPr,
	SFo
}
