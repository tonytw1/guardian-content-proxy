package nz.gen.wellington.guardian.contentapiproxy.model;

import nz.gen.wellington.guardian.model.Tag;

public class TagRefinement implements Refinement {

	private Tag tag;
	
	public TagRefinement(Tag tag) {
		this.tag = tag;
	}

	public Tag getTag() {
		return tag;
	}
	
}
