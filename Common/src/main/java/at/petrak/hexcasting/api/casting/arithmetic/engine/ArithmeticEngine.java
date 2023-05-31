package at.petrak.hexcasting.api.casting.arithmetic.engine;

import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic;
import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs;

import java.util.*;

public class ArithmeticEngine {
    private record OpCandidates(HexPattern pattern, int arity, List<Operator> operators) {
        public void addOp(Operator next) {
            if (next.arity != arity) {
                throw new IllegalArgumentException("Operators exist of differing arity!");
            }
            operators.add(next);
        }
    }

    public final Arithmetic[] arithmetics;
    private final Map<HexPattern, OpCandidates> operators = new HashMap<>();
    private final Map<HashCons, Operator> cache = new HashMap<>();

    public ArithmeticEngine(List<Arithmetic> arithmetics) {
        this.arithmetics = arithmetics.toArray(new Arithmetic[0]);
        for (var arith : arithmetics) {
            for (var op : arith.opTypes()) {
                operators.compute(op, ($, info) -> {
                    var operator = arith.getOperator(op);
                    if (info == null) {
                        info = new OpCandidates(op, operator.arity, new ArrayList<>());
                    }
                    info.addOp(operator);
                    return info;
                });
            }
        }
    }

    public Iterable<HexPattern> operatorSyms() {
        return operators.keySet();
    }

    public Iterable<Iota> run(HexPattern operator, Stack<Iota> iotas, int startingLength) throws Mishap {
        var candidates = operators.get(operator);
        if (candidates == null)
            throw new InvalidOperatorException("the pattern " + operator + " is not an operator."); //
        HashCons hash = new HashCons.Pattern(operator);
        var args = new ArrayList<Iota>(candidates.arity());
        for (var i = 0; i < candidates.arity(); i++) {
            if (iotas.isEmpty()) {
                throw new MishapNotEnoughArgs(candidates.arity, startingLength);
            }
            var iota = iotas.pop();
            hash = new HashCons.Pair(iota.getType(), hash);
            args.add(iota);
        }
        Collections.reverse(args);
        var op = resolveCandidates(args, hash, candidates);
        return op.apply(args);
    }

    public Operator resolveCandidates(List<Iota> args, HashCons hash, OpCandidates candidates) {
        return cache.computeIfAbsent(hash, $ -> {
            for (var op : candidates.operators()) {
                if (op.accepts.test(args)) {
                    return op;
                }
            }
            throw new NoOperatorCandidatesException(candidates.pattern(), args, "No implementation candidates for op " + candidates.pattern() + " on args: " + args);
        });
    }
}