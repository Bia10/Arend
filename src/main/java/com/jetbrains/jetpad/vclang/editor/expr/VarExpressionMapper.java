package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.property.Property;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.editor.util.Validators.identifier;
import static jetbrains.jetpad.cell.text.TextEditing.validTextEditing;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class VarExpressionMapper extends Mapper<VarExpression, TextCell> {
  public VarExpressionMapper(VarExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().addTrait(validTextEditing(identifier()));
    getTarget().addTrait(new CellTrait() {
      @Override
      public void onKeyTyped(Cell cell, KeyEvent event) {
        if (event.getKeyChar() == ' ') {
          if (((TextCell)cell).isEnd()) {
            AppExpression appExpr = new AppExpression();
            ((Property<Expression>) getSource().getPosition().getRole()).set(appExpr);
            appExpr.function.set(getSource());
            event.consume();
            return;
          }
        }
        super.onKeyTyped(cell, event);
      }
    });
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().name, getTarget().text()));
  }
}