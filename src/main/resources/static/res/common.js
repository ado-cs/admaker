function msgBox(title, content) {
    $('.basic.modal .header').html(title);
    $('.basic.modal .description').html('<p>' + content + '</p>');
    $('.basic.modal').modal('show')
}

const rule = {type: '选择广告类型', fee: '选择计费方式', name: '填写广告名', package: '选择版位', begin: '选择起始时间', end: '选择结束时间', amount: '填写广告数量'};

function initRules() {
    let dict = {};
    for (let k in rule) {
        dict[k] = {identifier: k, rules: [{type: 'empty', prompt: '请' + rule[k]}]}
    }
    $('.ui.form').form({fields: dict, inline: true, on: 'change',
        onSuccess: function(e) { e.preventDefault();}});
}

function initEvents() {
    let date = new Date();
    $('.ui.menu .item').tab();
    $('.ui.dropdown').dropdown();
    $('.ui.menu .item.home').click();
    $('input[name="name"]').val('压测' + date.getFullYear() + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + (date.getDate() < 10 ? '0' : '') + date.getDate());
    $('input[name="begin"]').val(date.getFullYear() + '-' + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + (date.getDate() < 10 ? '0' : '') + date.getDate());
    $('input[name="type"]').bind('change', function () {
        let deal = $('#deal');
        if ($(this).val() === '1') {
            $('input[name="fee"]').siblings('.menu').html('<div class="item" data-value="1">CPT</div><div class="item" data-value="2">CPM</div>');
            if (deal.hasClass('hidden')) {
                deal.removeClass('hidden');
                $('.ui.form').form('add rule', 'deal', {rules: [{type: 'empty', prompt: '请选择排期类型'}]})
            }
        }
        else {
            $('input[name="fee"]').siblings('.menu').html('<div class="item" data-value="3">CPC</div><div class="item" data-value="2">CPM</div>');
            if (!deal.hasClass('hidden')) {
                deal.addClass('hidden');
                $('.ui.form').form('remove field', 'deal')
            }
        }
    });
    $('.ui.blue.basic.inverted.button').bind('click', function () {
        $('.basic.modal').modal('hide')
    });
    $.fn.api.settings.api = {
        'create': '/j/create'
    };
    $('form .submit.button').api({
            action: 'create',
            serializeForm: true,
            method: 'post',
            beforeSend: function(settings) {
                if (!$('.ui.form').form('is valid')) return false;
                $('.ui.form').addClass('loading');
                return settings;
            },
            onResponse: function(response) {
                $('.ui.form').removeClass('loading');
                if (response) {
                    if (response.success) msgBox('创建成功', '广告单创建成功！');
                    else if (response.message) msgBox('创建失败', response.message);
                    else msgBox('创建失败', response.data + '个广告单创建失败！');
                }
                else msgBox('创建失败', '无响应！');
                return response;
            },
            onError: function(errorMessage) {
                msgBox('创建失败', errorMessage);
            }
        });

}

function initData() {
    $.ajax({
        url: '/j/list',
        type: 'get',
        success: function (data) {
            let html = '';
            for(let v of data) {
                html += '<option value="' + v.id + '">' + v.name + '</option>';
            }
            $('select[name="camp"]').html(html);
            $('.ui.form').removeClass('loading')
        },
        error: function(XMLHttpRequest) {
            if (XMLHttpRequest) msgBox(XMLHttpRequest.status, XMLHttpRequest.statusText)
        }
    })
}

$(document).ready(() => {
    initRules();
    initEvents();
    initData()
});



