package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import editormodels.UtilClassEditorModel;
import global.Util;
import models.*;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.testCaseEditor;
import views.html.testCaseView;
import views.html.utilClassEditor;
import views.html.utilClassView;

import static play.data.Form.form;


/**
 * Created by yerlibilgin on 03/05/15.
 */
public class UtilClassController extends Controller {
  public static final Form<UtilClassEditorModel> UTIL_CLASS_FORM = form(UtilClassEditorModel.class);

  public static Result getCreateUtilClassEditorView(Long groupId) {
    TestGroup testGroup = TestGroup.findById(groupId);
    if (testGroup == null) {
      return badRequest("Test group with id [" + groupId
          + "] not found!");
    } else {
      UtilClassEditorModel utilClassEditorModel = new UtilClassEditorModel();
      utilClassEditorModel.groupId = groupId;

      Form<UtilClassEditorModel> bind = UTIL_CLASS_FORM
          .fill(utilClassEditorModel);
      return ok(utilClassEditor.render(bind, false));
    }
  }

  public static Result doCreateUtilClass() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<UtilClassEditorModel> filledForm = UTIL_CLASS_FORM
        .bindFromRequest();

    if (filledForm.hasErrors()) {
      Util.printFormErrors(filledForm);
      return badRequest(utilClassEditor.render(filledForm, false));
    } else {
      UtilClassEditorModel model = filledForm.get();

      UtilClass utilClass = UtilClass.findByName(model.name);
      if (utilClass != null) {
        filledForm.reject("Atil class with name [" + utilClass.name
            + "] already exists");
        return badRequest(utilClassEditor.render(filledForm, false));
      }

      TestGroup tg = TestGroup.findById(model.groupId);
      if (tg == null) {
        filledForm.reject("No test group found with id [" + tg.id + "]");
        return badRequest(utilClassEditor.render(filledForm, false));
      }

      final User localUser = Application.getLocalUser(session());

      utilClass = new UtilClass();
      utilClass.name = model.name;
      utilClass.source = model.source;
      utilClass.shortDescription = model.shortDescription;
      utilClass.testGroup = tg;
      utilClass.owner = localUser;
      try {
        utilClass.save();
      } catch (Exception ex) {
        filledForm.reject("Compilation Failed [" + ex.getMessage() + "]");
        Logger.error(ex.getMessage(), ex);
        return badRequest(utilClassEditor.render(filledForm, false));
      }

      utilClass = UtilClass.findByName(utilClass.name);

      Logger.info("UtilClass with name " + utilClass.id + ":" + utilClass.name
          + " was created");
      return redirect(routes.GroupController.getGroupDetailView(tg.id, "utils"));
    }
  }

  public static Result getEditCaseEditorView(Long id) {
    UtilClass utilClass = UtilClass.findById(id);
    if (utilClass == null) {
      return badRequest("A utility class with id [" + id + "] was not found!");
    } else {
      UtilClassEditorModel ucModel = new UtilClassEditorModel();
      ucModel.id = id;
      ucModel.groupId = utilClass.testGroup.id;
      ucModel.name = utilClass.name;
      ucModel.shortDescription = utilClass.shortDescription;
      ucModel.source = utilClass.source;

      Form<UtilClassEditorModel> bind = UTIL_CLASS_FORM.fill(ucModel);
      return ok(utilClassEditor.render(bind, true));
    }
  }

  public static Result doEditUtilClass() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<UtilClassEditorModel> filledForm = UTIL_CLASS_FORM
        .bindFromRequest();

    if (filledForm.hasErrors()) {
      Util.printFormErrors(filledForm);
      return badRequest(utilClassEditor.render(filledForm, true));
    } else {
      UtilClassEditorModel model = filledForm.get();

      UtilClass uc = UtilClass.findById(model.id);
      if (uc == null) {
        filledForm.reject("A util class with ID [" + model.id
            + "] does not exist");
        return badRequest(utilClassEditor.render(filledForm, true));
      }

      uc.name = model.name;
      uc.shortDescription = model.shortDescription;
      uc.source = model.source;

      // check if the name is not duplicate
      UtilClass tmp = UtilClass.findByName(model.name);

      if (tmp == null || tmp.id == uc.id) {
        // either no such name or it is already this object. so update
        try {
          uc.update();
        }catch(Exception ex){
          Logger.error(ex.getMessage(), ex);
          filledForm.reject(ex.getMessage());
          return badRequest(utilClassEditor.render(filledForm, true));
        }
        Logger.info("Test Util Class " + uc.id + ":" + uc.name
            + " was updated");
        return redirect(routes.UtilClassController.viewUtilClass(uc.id));
      } else {
        filledForm.reject("The Name [" + model.name
            + "] is used by another test case");
        return badRequest(utilClassEditor.render(filledForm, true));
      }

    }
  }

  public static Result doDeleteUtilClass(Long id) {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());

    UtilClass uc = UtilClass.findById(id);
    if (uc == null) {
      // it does not exist. error
      return badRequest("A Util Class with id " + id + " does not exist.");
    }

    try {
      uc.delete();
    } catch (Exception ex) {
      ex.printStackTrace();
      Logger.error(ex.getMessage(), ex);
      return badRequest(ex.getMessage());
    }
    return redirect(routes.GroupController.getGroupDetailView(uc.testGroup.id, "utils"));
  }

  public static Result viewUtilClass(long id) {
    UtilClass uc = UtilClass.findById(id);
    if (uc == null) {
      return badRequest("No util class id " + id + ".");
    }
    return ok(utilClassView.render(uc));
  }

  public static Result doEditUtilClassField() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    JsonNode jsonNode = request().body().asJson();

    Result res = GroupController.doEditField(UtilClassEditorModel.class, UtilClass.class, jsonNode);

    if (res.toScala().header().status() == BAD_REQUEST) {
      return res;
    } else {
      long id = jsonNode.findPath("id").asInt();
      UtilClass uc = UtilClass.findById(id);
      //just trigger recompile and stuff
      try {
        uc.save();
        return res;
      } catch (Exception ex) {
        Logger.error(ex.getMessage(), ex);
        return badRequest("Compilation Failed [" + ex.getMessage() + "]");
      }
    }
  }
}