package securitycontract;

import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.AionSet;
import avm.Address;
import org.aion.avm.userlib.abi.ABIDecoder;

public class contract1 {
    private static AionSet<Address> members = new AionSet<>();
    private static AionMap<Integer, Proposal> proposals = new AionMap<>();
    private static Address owner;
    private static int minimumQuorum;

    static {
        ABIDecoder decoder = new ABIDecoder(Blockchain.getData());

        Address[] arg = decoder.decodeOneAddressArray();
        for (Address addr:arg) {
            members.add(addr);
        }
        updateQuorum();
        owner = Blockchain.getCaller();
    }

    @Callable
    public static void addProposal(String description) {
        Blockchain.require(isMember(Blockchain.getCaller()));

        Proposal proposal = new Proposal(description, Blockchain.getCaller());
        int proposalId = proposals.size();
        proposals.put(proposalId, proposal);

        Blockchain.log("NewProposalAdded".getBytes(), Integer.toString(proposalId).getBytes(), Blockchain.getCaller().unwrap(),     description.getBytes());
    }

    @Callable
    public static void vote(int proposalId) {
        Blockchain.require(isMember(Blockchain.getCaller()) && proposals.containsKey(proposalId));

        Proposal votedProposal = proposals.get(proposalId);
        votedProposal.voters.add(Blockchain.getCaller());

        Blockchain.log("Voted".getBytes(), Integer.toString(proposalId).getBytes(), Blockchain.getCaller().unwrap());

        if (!votedProposal.isPassed && votedProposal.voters.size() == minimumQuorum) {
            votedProposal.isPassed = true;
            Blockchain.log("ProposalPassed".getBytes(), Integer.toString(proposalId).getBytes());
        }
    }

    @Callable
    public static void addMember(Address newMember) {
        onlyOwner();
        members.add(newMember);
        updateQuorum();
        Blockchain.log("MemberAdded".getBytes(), newMember.unwrap());
    }

    @Callable
    public static void removeMember(Address member) {
        onlyOwner();
        members.remove(member);
        updateQuorum();
        Blockchain.log("MemberRemoved".getBytes(), member.unwrap());
    }

    @Callable
    public static String getProposalDescription(int proposalId) {
        return proposals.containsKey(proposalId) ? proposals.get(proposalId).description : null;
    }

    @Callable
    public static Address getProposalOwner(int proposalId) {
        return proposals.containsKey(proposalId) ? proposals.get(proposalId).owner : null;
    }

    @Callable
    public static boolean hasProposalPassed(int proposalId) {
        return proposals.containsKey(proposalId) && proposals.get(proposalId).isPassed;
    }

    @Callable
    public static int getMinimumQuorum() {
        return minimumQuorum;
    }

    @Callable
    public static boolean isMember(Address address) {
        return members.contains(address);
    }

    private static void onlyOwner() {
        Blockchain.require(owner.equals(Blockchain.getCaller()));
    }

    private static void updateQuorum() {
        minimumQuorum = members.size() / 2 + 1;
    }

    private static class Proposal {
        String description;
        Address owner;
        boolean isPassed;
        AionSet<Address> voters = new AionSet<>();

        Proposal(String description, Address owner) {
            this.description = description;
            this.owner = owner;
        }
    }
}